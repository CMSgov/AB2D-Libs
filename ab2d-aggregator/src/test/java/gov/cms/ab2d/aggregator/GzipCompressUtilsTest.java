package gov.cms.ab2d.aggregator;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GzipCompressUtilsTest {

    /**
        Test file contains first 500 entries from https://bcda.cms.gov/assets/data/ExplanationOfBenefit.ndjson
     */
    public static final Path UNCOMPRESSED_FILE = Path.of("src/test/resources/test-data/EOB-500.ndjson");
    /**
        Test file compressed using the 'gzip' command line utility, not {@link GzipCompressUtils#compress}
     */
    public static final Path COMPRESSED_FILE_USING_GZIP_CLI = Path.of("src/test/resources/test-data/EOB-500.ndjson.gz");

    @Test
    void testCompressFile() throws Exception {
        Path outputCompressed = newTestFile(".ndjson.gz");
        GzipCompressUtils.compress(UNCOMPRESSED_FILE, outputCompressed);

        /**
            Note that the following is not a valid assertion because the 'gzip' command line utility can produce
            output that differs from {@link GzipCompressUtils#compress}, however both outputs are valid.

            assertTrue(
                FileUtils.contentEquals(output.toFile(),
                COMPRESSED_FILE.toFile())
            );

            Instead, decompress the `outputCompressed` file and assert it matches {@link UNCOMPRESSED_FILE}
         */

        Path outputDecompressed = newTestFile(".ndjson");
        GzipCompressUtils.decompress(outputCompressed, outputDecompressed);

        assertTrue(
            FileUtils.contentEquals(
                outputDecompressed.toFile(),
                UNCOMPRESSED_FILE.toFile()
            )
        );
    }

    @Test
    void testCompressOutputStream() throws Exception {
        ByteArrayOutputStream outputCompressed = new ByteArrayOutputStream();
        GzipCompressUtils.compress(UNCOMPRESSED_FILE, outputCompressed);

        ByteArrayOutputStream outputUncompressed = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(
                new ByteArrayInputStream(outputCompressed.toByteArray()),
                outputUncompressed
        );

        assertArrayEquals(
            Files.readAllBytes(UNCOMPRESSED_FILE),
            outputUncompressed.toByteArray()
        );
    }

    @Test
    void testDecompressFile() throws Exception {
        Path output = newTestFile(".ndjson");
        GzipCompressUtils.decompress(COMPRESSED_FILE_USING_GZIP_CLI, output);
        assertTrue(
            FileUtils.contentEquals(
                output.toFile(),
                UNCOMPRESSED_FILE.toFile()
            )
        );
    }

    @Test
    void testDecompressOutputStream() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(COMPRESSED_FILE_USING_GZIP_CLI, output);
        assertArrayEquals(
            Files.readAllBytes(UNCOMPRESSED_FILE),
            output.toByteArray()
        );
    }

    Path newTestFile(String suffix) throws IOException {
        Path file = Files.createTempFile(getClass().getSimpleName(), suffix);
        file.toFile().deleteOnExit();
        return file;
    }

}