package gov.cms.ab2d.aggregator;

import lombok.experimental.UtilityClass;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class GzipCompressUtils {

    public static void compress(final InputStream inputStream, final OutputStream out) throws IOException {
        try (BufferedOutputStream outputBuffer = new BufferedOutputStream(out);
             GzipCompressorOutputStream compressor = new GzipCompressorOutputStream(outputBuffer)) {
            org.apache.commons.io.IOUtils.copy(inputStream, compressor);
        }
    }

    public static void compress(final Path uncompressedFile, final OutputStream out) throws IOException {
        try (InputStream inputStream = Files.newInputStream(uncompressedFile)) {
            compress(inputStream, out);
        }
    }

    public static void compress(final Path uncompressedFile, final Path destination) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination)) {
            compress(uncompressedFile, out);
        }
    }

    public static void decompress(final InputStream inputStream, final OutputStream out) throws IOException {
        try (BufferedInputStream inputBuffer = new BufferedInputStream(inputStream);
             GzipCompressorInputStream decompressor = new GzipCompressorInputStream(inputBuffer)) {
            org.apache.commons.io.IOUtils.copy(decompressor, out);
        }
    }

    public static void decompress(final Path compressedFile, final OutputStream out) throws IOException {
        try (InputStream inputStream = Files.newInputStream(compressedFile)) {
            decompress(inputStream, out);
        }
    }

    public static void decompress(final Path compressedFile, Path destination) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination)) {
            decompress(compressedFile, out);
        }
    }
}