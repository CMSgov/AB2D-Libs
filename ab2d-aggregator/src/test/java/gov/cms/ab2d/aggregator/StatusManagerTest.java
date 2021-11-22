package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static gov.cms.ab2d.aggregator.ConfigManager.ONE_MEGA_BYTE;
import static gov.cms.ab2d.aggregator.FileUtils.createADir;
import static gov.cms.ab2d.aggregator.FileUtils.deleteAllInDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusManagerTest {
    private final transient String tmpdir = System.getProperty("java.io.tmpdir");
    private static final String ABC = "abc";
    private static final String JOB_DOWNLOADS = "jobdownloads/";

    @Test
    void testVars() {
        assertEquals(2, ConfigManager.getMultiplier());
        assertEquals(tmpdir + JOB_DOWNLOADS + "abc/streaming/", ConfigManager.getFileStreamingDirectory(ABC));
        assertEquals(tmpdir + JOB_DOWNLOADS + "abc/finished/", ConfigManager.getFileDoneDirectory(ABC));
        assertEquals(ONE_MEGA_BYTE, ConfigManager.getMaxFileSize());
    }

    @Test
    void testIsJobDone() throws IOException {
        String jobId = ABC;
        File testdir = createADir(tmpdir + JOB_DOWNLOADS + jobId);
        try {
            assertTrue(StatusManager.isJobDoneStreamingData(jobId));
            File testdirStreaming = createADir(tmpdir + JOB_DOWNLOADS + jobId + "/streaming");
            assertFalse(StatusManager.isJobDoneStreamingData(jobId));
            deleteAllInDir(testdirStreaming);
            assertTrue(StatusManager.isJobDoneStreamingData(jobId));
        } finally {
            assertTrue(deleteAllInDir(testdir));
        }
    }

    @Test
    void testIsJobAggregated() throws IOException {
        String jobId = ABC;
        File testdir = createADir(tmpdir + JOB_DOWNLOADS + jobId);
        try {
            File testdirDone = createADir(tmpdir + JOB_DOWNLOADS + jobId + "/finished");
            assertTrue(StatusManager.isJobAggregated(jobId));
            Path path1 = FileUtilsTest.createFile(testdirDone, "file1.ndjson", ABC);
            assertFalse(StatusManager.isJobAggregated(jobId));
            assertTrue(path1.toFile().delete());
            assertTrue(StatusManager.isJobAggregated(jobId));
        } finally {
            assertTrue(deleteAllInDir(testdir));
            assertTrue(StatusManager.isJobAggregated(jobId));
        }
    }
}