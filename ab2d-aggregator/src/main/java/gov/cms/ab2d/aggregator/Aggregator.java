package gov.cms.ab2d.aggregator;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static gov.cms.ab2d.aggregator.ConfigManager.getFileDoneDirectory;
import static gov.cms.ab2d.aggregator.ConfigManager.getJobDirectory;
import static gov.cms.ab2d.aggregator.ConfigManager.getMaxFileSize;
import static gov.cms.ab2d.aggregator.ConfigManager.getMultiplier;
import static gov.cms.ab2d.aggregator.FileUtils.cleanUpFiles;
import static gov.cms.ab2d.aggregator.FileUtils.combineFiles;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFileOrDirectory;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFiles;
import static gov.cms.ab2d.aggregator.FileUtils.listFiles;
import static gov.cms.ab2d.aggregator.StatusManager.isJobDoneStreamingData;

/**
 * Does the work of aggregating files
 */
@Getter
public class Aggregator {

    private final String jobId;
    private final String mainDirectory;
    private final String contractNumber;
    private int currentFileIndex = 1;
    private int currentErrorFileIndex = 1;

    /**
     * Define the Aggregator for the job
     *
     * @param jobId - the job ID
     * @param contractNumber - the contract number (used for naming files)
     */
    public Aggregator(String jobId, String contractNumber) {
        this.jobId = jobId;
        this.mainDirectory = getJobDirectory(jobId);
        this.contractNumber = contractNumber;
    }

    /**
     * Aggregate! This method finds the best files to aggregate, combines those files
     * and deletes the temporary files created by the worker. This will only do
     * one aggregation. The goal is to run this method until it returns false
     *
     * @param error - if we want to aggregate error files or data files
     * @return true if there are enough files to aggregate (or the worker is done writing data)
     *      and we then did aggregate, false if there aren't enough files to aggregate.
     * @throws IOException if one of this file manipulations fails
     */
    public boolean aggregate(boolean error) throws IOException {
        if (!okayToDoAggregation(error)) {
            return false;
        }
        List<File> bestFiles = getBestFiles(error);
        if (bestFiles.isEmpty()) {
            return false;
        }
        String fileName = getNextFileName(error);
        combineFiles(bestFiles, fileName);
        cleanUpFiles(bestFiles);
        return true;
    }

    /**
     * Return the name of the next file to create given it is or isn't an error file and the
     * last file index
     *
     * @param error - if this is an error file
     * @return the name of the next file to aggregate into
     */
    String getNextFileName(boolean error) {
        String namePart = error ? getNextErrorFileName() : getNextDataFileName();
        return mainDirectory + namePart;
    }

    /**
     * If we should aggregate files
     *
     * @param error - if we are talking about error files or not
     * @return - true if we have enough files or the worker is done writing out files
     */
    boolean okayToDoAggregation(boolean error) {
        long size = getSizeOfFiles(getFileDoneDirectory(this.jobId), error);
        return size > getMultiplier() * getMaxFileSize() || isJobDoneStreamingData(this.jobId);
    }

    String getNextDataFileName() {
        String partName = Integer.toString(currentFileIndex);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        currentFileIndex++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                FileOutputType.NDJSON.getSuffix();
    }

    String getNextErrorFileName() {
        String partName = Integer.toString(currentErrorFileIndex);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        currentErrorFileIndex++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                FileOutputType.NDJSON_ERROR.getSuffix();
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
     * @param error whether we are writing error files
     * @return the list of "best" files to combine to optimize fullness of individual files
     */
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.DataflowAnomalyAnalysis"})
    List<File> getBestFiles(boolean error) {
        // Get all the files and their sizes in sorted order
        List<FileDescriptor> sortedFiles = getSortedFileDescriptors(error);

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

        List<FileDescriptor> bestFiles = new ArrayList<>();
        long totalFileSize = 0L;

        // Add the large files first - iterate backwards until we're over the top on the next item
        // Remove items as we go. Find the first next biggest files that will add up to the max file size
        ListIterator<FileDescriptor> bigIter = sortedFiles.listIterator(sortedFiles.size());
        while (bigIter.hasPrevious()) {
            FileDescriptor fd = bigIter.previous();
            if (totalFileSize + fd.getSize() > getMaxFileSize()) {
                continue;
            }
            totalFileSize += fd.getSize();
            bestFiles.add(fd);
            bigIter.remove();
        }

        return bestFiles.stream().map(FileDescriptor::getFile).collect(Collectors.toList());
    }

    List<FileDescriptor> orderBySize(List<FileDescriptor> files) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().sorted(Comparator.comparingLong(FileDescriptor::getSize)).collect(Collectors.toList());
    }

    /**
     * Get file descriptors for each file. This allows us to do comparisons and sorting by
     * file size without having to check with the file system each time
     *
     * @param error - if we are doing error files
     * @return the list of file descriptors with the relevant extensions
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    List<FileDescriptor>  getSortedFileDescriptors(boolean error) {
        List<File> availableFiles = listFiles(getFileDoneDirectory(this.jobId), error);

        List<FileDescriptor> files = new ArrayList<>();
        availableFiles.forEach(f -> {
            long size;
            try {
                size = getSizeOfFileOrDirectory(f.getAbsolutePath());
            } catch (IOException e) {
                size = 0L;
            }
            files.add(new FileDescriptor(f, size));
        });

        // Order the files by file size
        return orderBySize(files);
    }
}
