package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BeneficiaryStreamTest {

    private static final String JOB_ID = "job1";
    private static final String STREAM_DIR = "streaming";
    private static final String FINISH_DIR = "finished";

    @Test
    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.CloseResource"})
    void testCreateAndWriteToStream(@TempDir File tmpDirFolder) {
        BeneficiaryStream savedStream = null;
        try (BeneficiaryStream stream = new BeneficiaryStream(JOB_ID, tmpDirFolder.getAbsolutePath(), false, STREAM_DIR, FINISH_DIR)) {
            savedStream = stream;
            for (int i = 0; i < 1000; i++) {
                stream.write(AggregatorTest.getAlphaNumericString(1000));
            }

            File tmpFile = stream.getFile();
            assertTrue(tmpFile.exists());
            assertTrue(stream.isOpen());
            File tmpFileDirectory = new File(tmpDirFolder.getAbsolutePath() + "/" + JOB_ID + "/" + STREAM_DIR);
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());

        } catch (Exception ex) {
            fail(ex);
        }
        if (savedStream != null) {
            File tmpFile = savedStream.getFile();
            assertTrue(tmpFile.exists());
            assertFalse(savedStream.isOpen());
            File tmpFileDirectory = new File(tmpDirFolder.getAbsolutePath() + "/" + JOB_ID + "/" + FINISH_DIR);
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());
        }
    }
}