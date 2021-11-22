package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BeneficiaryStreamTest {

    private static final String JOB_ID = "job1";

    @AfterEach
    void cleanUp() {
        File fileTmp = new File(ConfigManager.getFileStreamingDirectory(JOB_ID));
        if (fileTmp.exists()) {
            File[] files = fileTmp.listFiles();
            if (files != null) {
                for (File f : files) {
                    assertTrue(f.delete());
                }
            }
        }
        File tmpFDoneDir = new File(ConfigManager.getFileDoneDirectory(JOB_ID));
        if (tmpFDoneDir.exists()) {
            File[] files = tmpFDoneDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    assertTrue(f.delete());
                }
            }
        }

    }

    @Test
    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.CloseResource"})
    void testCreateAndWriteToStream() {
        BeneficiaryStream savedStream = null;
        try (BeneficiaryStream stream = new BeneficiaryStream(JOB_ID, false)) {
            savedStream = stream;
            for (int i = 0; i < 1000; i++) {
                stream.write(AggregatorTest.getAlphaNumericString(1000));
            }

            File tmpFile = stream.getFile();
            assertTrue(tmpFile.exists());
            assertTrue(stream.isOpen());
            File tmpFileDirectory = new File(ConfigManager.getFileStreamingDirectory(JOB_ID));
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());

        } catch (Exception ex) {
            fail(ex);
        }
        if (savedStream != null) {
            File tmpFile = savedStream.getFile();
            assertTrue(tmpFile.exists());
            assertFalse(savedStream.isOpen());
            File tmpFileDirectory = new File(ConfigManager.getFileDoneDirectory(JOB_ID));
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());
        }
    }
}