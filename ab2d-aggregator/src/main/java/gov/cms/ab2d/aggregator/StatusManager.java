package gov.cms.ab2d.aggregator;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.cms.ab2d.aggregator.ConfigManager.getFileDoneDirectory;
import static gov.cms.ab2d.aggregator.ConfigManager.getFileStreamingDirectory;

/**
 * This hides the guts of how we know the status of a job based on the directory which exist
 */
@Slf4j
public class StatusManager {

    /**
     * Has the worker finished streaming out data for the job?
     *
     * @param jobId - the job id
     * @return true if the worker has indicated they are done streaming data
     */
    public static boolean isJobDoneStreamingData(String jobId) {
        String streamingDir = getFileStreamingDirectory(jobId);
        boolean fileExists = Files.exists(Path.of(streamingDir));
        // Job is done if dir doesn't exist
        return !fileExists;
    }

    /**
     * Has the aggregator finished doing all its aggregation?
     *
     * @param jobId - the job id
     * @return true if the aggregator has indicated that it has finished combining all outputted worker files.
     * This will always be false if the worker is not done streaming
     */
    public static boolean isJobAggregated(String jobId) {
        // If job isn't done, we can't be done aggregating
        if (!isJobDoneStreamingData(jobId)) {
            return false;
        }
        // Look for the files in the done writing directory
        String finishedDir = getFileDoneDirectory(jobId);
        Path dirPath = Path.of(finishedDir);
        // If the directory doesn't exist, yes we're done, but shouldn't happen
        boolean dirExists = Files.exists(dirPath);
        if (!dirExists) {
            return true;
        }
        // Get the contents. If there are no files to aggregate, we're done.
        File[] contents = dirPath.toFile().listFiles();
        return contents == null || contents.length == 0;
    }
}
