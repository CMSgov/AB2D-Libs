package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.aggregator.ConfigManager.ONE_MEGA_BYTE;
import static gov.cms.ab2d.aggregator.FileUtils.createADir;
import static gov.cms.ab2d.aggregator.FileUtils.deleteAllInDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusManagerTest {
    private final transient String tmpdir = System.getProperty("java.io.tmpdir");
    private static final String ABC = "abc";
    private static final String JOB_DOWNLOADS = "/jobdownloads/";

    @Test
    void testVars() {
        assertEquals(2, ConfigManager.getMultiplier());
        assertEquals(new File(tmpdir + JOB_DOWNLOADS + ABC + "/streaming/").getAbsolutePath(),
                new File(ConfigManager.getFileStreamingDirectory(ABC)).getAbsolutePath());
        assertEquals(new File(tmpdir + JOB_DOWNLOADS + ABC + "/finished/").getAbsolutePath(),
                new File(ConfigManager.getFileDoneDirectory(ABC)).getAbsolutePath());
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

    @Test
    void testGetFiles() throws IOException {
        String job = ABC;
        String jobDir = ConfigManager.getJobDirectory(job);
        File dirFile = new File(jobDir);
        assertTrue(dirFile.mkdirs());

        List<File> dataFiles = StatusManager.getFiles(ABC, false);
        List<File> errorFiles = StatusManager.getFiles(ABC, true);
        assertEquals(0, dataFiles.size());
        assertEquals(0, errorFiles.size());

        AggregatorTest.writeToFile(jobDir + "/S001_0001" + FileOutputType.NDJSON.getSuffix(), 10);
        dataFiles = StatusManager.getFiles(job, false);
        errorFiles = StatusManager.getFiles(job, true);
        assertEquals(1, dataFiles.size());
        assertEquals(0, errorFiles.size());

        AggregatorTest.writeToFile(jobDir + "/S001_0002" + FileOutputType.NDJSON_ERROR.getSuffix(), 10);
        dataFiles = StatusManager.getFiles(job, false);
        errorFiles = StatusManager.getFiles(job, true);
        assertEquals(1, dataFiles.size());
        assertEquals(1, errorFiles.size());
    }
}