package gov.cms.ab2d.aggregator;

import java.io.IOException;

import static gov.cms.ab2d.aggregator.FileUtils.createADir;
import static gov.cms.ab2d.aggregator.FileUtils.deleteAllInDir;

/**
 * This class facilitates communication between the "worker" and the "aggregator". Currently, this is done
 * by the presence of different directories. When a job is created using workerSetUpJobDirectories, it creates
 * three directories. One for the job, one for any open streaming files and one for any finished streaming files
 *
 * The aggregator knows that there are still files to be streamed if the open streaming directory still exists.
 * The worker knows that there are still files to be aggregated if the finished streaming directory still exists.
 */
public final class JobHelper {
    private JobHelper() { }

    /**
     * This does the work of creating all the necessary directories for the job
     * @param jobId - the job id and the root directory
     * @throws IOException if there is a problem creating the directories
     */
    static void workerSetUpJobDirectories(String jobId) throws IOException {
        // Create job directory
        createADir(ConfigManager.getJobDirectory(jobId));

        // Create the directory where we're going to put all the finished streams
        createADir(ConfigManager.getFileDoneDirectory(jobId));

        // Create a directory that we're going to dump all the streaming files
        createADir(ConfigManager.getFileStreamingDirectory(jobId));
    }


    /**
     * This allows the worker to send a message to the aggregator that it is done streaming data. This
     * deletes the streaming directory
     *
     * @param jobId - the job id
     */
    static void workerFinishJob(String jobId) {
        deleteAllInDir(ConfigManager.getFileStreamingDirectory(jobId));
    }

    /**
     * This allows the aggregator to let the job know that the aggregator has finished aggregating EOB files
     *
     * @param jobId - the job id
     */
    public static void aggregatorFinishJob(String jobId) {
        deleteAllInDir(ConfigManager.getFileDoneDirectory(jobId));
    }
}
