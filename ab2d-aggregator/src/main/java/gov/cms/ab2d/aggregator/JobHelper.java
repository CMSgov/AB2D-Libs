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
    public static void workerSetUpJobDirectories(String jobId, String baseDir, String streamDir, String finishedDir) throws IOException {
        // Create job directory
        createADir(baseDir + "/" + jobId);

        // Create the directory where we're going to put all the finished streams
        createADir(baseDir + "/" + jobId + "/" + streamDir);

        // Create a directory that we're going to dump all the streaming files
        createADir(baseDir + "/" + jobId + "/" + finishedDir);
    }


    /**
     * This allows the worker to send a message to the aggregator that it is done streaming data. This
     * deletes the streaming directory
     *
     * @param streamingDir - the location where all finished files are put by the worker
     */
    public static void workerFinishJob(String streamingDir) {
        deleteAllInDir(streamingDir);
    }

    /**
     * This allows the aggregator to let the job know that the aggregator has finished aggregating EOB files
     *
     * @param finishedDir - the job id
     */
    public static void aggregatorFinishJob(String finishedDir) {
        deleteAllInDir(finishedDir);
    }
}
