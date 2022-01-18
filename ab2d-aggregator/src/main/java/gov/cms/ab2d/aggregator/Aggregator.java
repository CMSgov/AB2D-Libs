package gov.cms.ab2d.aggregator;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static gov.cms.ab2d.aggregator.Aggregator.AggregatorResult.NOT_PERFORMED;
import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.aggregator.FileUtils.cleanUpFiles;
import static gov.cms.ab2d.aggregator.FileUtils.combineFiles;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFileOrDirectory;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFiles;
import static gov.cms.ab2d.aggregator.FileUtils.listFiles;

/**
 * Does the work of aggregating files
 */
@Getter
public class Aggregator {

    public enum AggregatorResult {
        PERFORMED,
        NOT_PERFORMED
    }

    public static final int ONE_MEGA_BYTE = 1024 * 1024;

    private final String jobId;
    private final String mainDirectory;
    private final String contractNumber;
    private final String streamDir;
    private final String finishedDir;
    private final int maxMegaByes;
    private final int multiplier;

    private int currentFileIndex = 1;
    private int currentErrorFileIndex = 1;

    /**
     * Define the Aggregator for the job
     *
     * @param jobId - the job ID
     * @param contractNumber - the contract number (used for naming files)
     */
    public Aggregator(String jobId, String contractNumber, String fileDir, int maxMegaBytes, String streamDir,
                      String finishedDir, int multiplier) throws IOException {
        this.jobId = jobId;
        this.mainDirectory = fileDir + "/" + jobId;
        this.contractNumber = contractNumber;
        this.streamDir = streamDir;
        this.maxMegaByes = maxMegaBytes;
        this.finishedDir = finishedDir;
        this.multiplier = multiplier;

        // The worker should do this by default, but just in case, set up all the directories
        JobHelper.workerSetUpJobDirectories(jobId, fileDir, streamDir, finishedDir);
    }

    /**
     * Aggregate! This method finds the best files to aggregate, combines those files
     * and deletes the temporary files created by the worker. This will only do
     * one aggregation. The goal is to run this method until it returns false
     *
     * @param fileType - type of file to aggregate
     * @return true if there are enough files to aggregate (or the worker is done writing data)
     *      and we then did aggregate, false if there aren't enough files to aggregate.
     * @throws IOException if one of this file manipulations fails
     */
    public AggregatorResult aggregate(FileOutputType fileType) throws IOException {
        // remove any empty files
        removeEmptyFiles();

        if (!okayToDoAggregation(fileType)) {
            return NOT_PERFORMED;
        }
        List<File> bestFiles = getBestFiles(fileType);
        if (bestFiles.isEmpty()) {
            return NOT_PERFORMED;
        }
        String fileName = getNextFileName(fileType);
        if (fileName == null) {
            return NOT_PERFORMED;
        }
        combineFiles(bestFiles, fileName);
        cleanUpFiles(bestFiles);
        return AggregatorResult.PERFORMED;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    void removeEmptyFiles() {
        String finishedDir = this.mainDirectory + "/" + this.finishedDir;
        List<File> availableFiles = listFiles(finishedDir, DATA);
        availableFiles.addAll(listFiles(finishedDir, ERROR));
        List<File> emptyFiles = availableFiles.stream()
                .filter(f -> {
                            long size;
                            try {
                                size = getSizeOfFileOrDirectory(f.getAbsolutePath());
                            } catch (IOException e) {
                                size = 0L;
                            }
                            return size == 0;
                        }).collect(Collectors.toList());
        emptyFiles.forEach(File::delete);
    }

    /**
     * Return the name of the next file to create given it is or isn't an error file and the
     * last file index
     *
     * @param type - type of output file
     * @return the name of the next file to aggregate into
     */
    String getNextFileName(FileOutputType type) {
        switch (type) {
            case ERROR:
                return Path.of(mainDirectory, getNextErrorFileName()).toFile().getAbsolutePath();
            case DATA:
                return Path.of(mainDirectory, getNextDataFileName()).toFile().getAbsolutePath();
            default:
                return null;
        }
    }

    /**
     * If we should aggregate files
     *
     * @param type - type of file
     * @return - true if we have enough files or the worker is done writing out files
     */
    boolean okayToDoAggregation(FileOutputType type) {
        long size = getSizeOfFiles(this.mainDirectory + "/" + this.finishedDir, type);
        return (size > ((long) this.multiplier * getMaxFileSize())) || isJobDoneStreamingData();
    }

    String getNextDataFileName() {
        String partName = Integer.toString(currentFileIndex);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        currentFileIndex++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                DATA.getSuffix();
    }

    String getNextErrorFileName() {
        String partName = Integer.toString(currentErrorFileIndex);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        currentErrorFileIndex++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                ERROR.getSuffix();
    }

    /**
     * We attempt to find the best combination of files to aggregate. This isn't a complicated
     * algorithm. It sorts the files by size and grabs files from largest to smallest. If it gets
     * to a file that will make it exceed the max size, it skips that file and goes to the next file
     * and attempts to include that. It continues down the entire list.
     *
     * For example, if we * had a list with file sizes: 9, 5, 3, 2 and we had a max size of 13,
     * we'd first grab 9, but 5 would put us over the top so we skip it, we'd keep 3 because that
     * wouldn't be more than * 13, but the next value 2 puts us over the top. There are probably
     * better algorithms but this gets the job done in a reasonably efficient manner.
     *
     * @param type type of file
     * @return the list of "best" files to combine to optimize fullness of individual files
     */
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.DataflowAnomalyAnalysis"})
    List<File> getBestFiles(FileOutputType type) {
        // Get all the files and their sizes in sorted order
        List<FileReferenceHolder> sortedFiles = getSortedFileReferences(type);

        // If there are no files, return an empty list
        if (sortedFiles == null || sortedFiles.isEmpty()) {
            return new ArrayList<>();
        }

        // If there is only one file, return it - we have no other choice
        if (sortedFiles.size() == 1) {
            return List.of(sortedFiles.get(0).getFile());
        }

        // If the largest file is larger than the max file size, it's not great but should
        // be returned
        if (sortedFiles.get(sortedFiles.size() - 1).getSize() > getMaxFileSize()) {
            return List.of(sortedFiles.get(sortedFiles.size() - 1).getFile());
        }

        List<FileReferenceHolder> bestFiles = new ArrayList<>();
        long totalFileSize = 0L;

        // Add the large files first - iterate backwards until we're over the top on the next item
        // Remove items as we go. Find the first next biggest files that will add up to the max file size
        ListIterator<FileReferenceHolder> bigIter = sortedFiles.listIterator(sortedFiles.size());
        while (bigIter.hasPrevious()) {
            FileReferenceHolder fd = bigIter.previous();
            if (totalFileSize + fd.getSize() > getMaxFileSize()) {
                continue;
            }
            totalFileSize += fd.getSize();
            bestFiles.add(fd);
            bigIter.remove();
        }

        return bestFiles.stream().map(FileReferenceHolder::getFile).collect(Collectors.toList());
    }

    List<FileReferenceHolder> orderBySize(List<FileReferenceHolder> files) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().sorted(Comparator.comparingLong(FileReferenceHolder::getSize)).collect(Collectors.toList());
    }

    /**
     * Get file descriptors for each file. This allows us to do comparisons and sorting by
     * file size without having to check with the file system each time
     *
     * @param type - type of file
     * @return the list of file descriptors with the relevant extensions
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    List<FileReferenceHolder> getSortedFileReferences(FileOutputType type) {
        List<File> availableFiles = listFiles(this.mainDirectory + "/" + this.finishedDir, type);

        List<FileReferenceHolder> files = new ArrayList<>();
        availableFiles.forEach(f -> {
            long size;
            try {
                size = getSizeOfFileOrDirectory(f.getAbsolutePath());
            } catch (IOException e) {
                size = 0L;
            }
            files.add(new FileReferenceHolder(f, size));
        });

        // Order the files by file size
        return orderBySize(files);
    }

    public boolean isJobDoneStreamingData() {
        String streamingDir = this.mainDirectory + "/" + this.streamDir;
        boolean fileExists = dirExists(streamingDir);
        // Job is done if dir doesn't exist
        return !fileExists;
    }

    private boolean dirExists(String dir) {
        return Files.exists(Path.of(dir));
    }

    /**
     * Has the aggregator finished doing all its aggregation?
     *
     * @return true if the aggregator has indicated that it has finished combining all outputted worker files.
     * This will always be false if the worker is not done streaming
     */
    public boolean isJobAggregated() {
        // If job isn't done, we can't be done aggregating
        if (dirExists(this.mainDirectory + "/" + this.streamDir)) {
            return false;
        }
        // Look for the files in the done writing directory
        return !dirExists(this.mainDirectory + "/" + this.finishedDir);
    }

    public int getMaxFileSize() {
        return this.maxMegaByes * ONE_MEGA_BYTE;
    }
}
