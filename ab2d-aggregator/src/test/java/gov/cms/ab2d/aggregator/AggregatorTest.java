package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.aggregator.ConfigManager.getFileDoneDirectory;
import static gov.cms.ab2d.aggregator.ConfigManager.getFileStreamingDirectory;
import static gov.cms.ab2d.aggregator.ConfigManager.getJobDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatorTest {
    @SuppressWarnings("checkstyle:VisibilityModifier")
    @TempDir
    transient File tmpDir;

    private static String jobId = "job123";
    private static String contractNum = "contractNum";
    private static Aggregator aggregator = new Aggregator(jobId, contractNum);
    private static String jobDoneStreamDir = getFileDoneDirectory(jobId);
    private static String finalDir = getJobDirectory(jobId);
    private static String fileStreamingDir = getFileStreamingDirectory(jobId);

    private static String f1Data = "f1.ndjson";
    private static String f2Data = "f2.ndjson";
    private static String f3Data = "f3.ndjson";
    private static String f4Data = "f4.ndjson";
    private static String data1Ext = "_0001.ndjson";
    private static String data2Ext = "_0002.ndjson";

    private static final SecureRandom RANDOM = new SecureRandom();

    @BeforeEach
    void createDirs() throws IOException {
        File jobDoneDir = new File(jobDoneStreamDir);
        if (!jobDoneDir.exists()) {
            Files.createDirectories(Path.of(jobDoneDir.getAbsolutePath()));
        }
        File jobStreamDir = new File(fileStreamingDir);
        if (!jobStreamDir.exists()) {
            Files.createDirectories(Path.of(jobStreamDir.getAbsolutePath()));
        }
    }

    @AfterEach
    void deleteFiles() throws IOException {
        File jobDoneDir = new File(jobDoneStreamDir);
        File[] files = jobDoneDir.listFiles();
        if (files != null) {
            for (File f : files) {
                assertTrue(f.delete());
            }
        }
        File jobStreamDir = new File(fileStreamingDir);
        if (jobStreamDir.exists()) {
            assertTrue(jobStreamDir.delete());
        }
        File finalFileDir = new File(finalDir);
        files = finalFileDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory()) {
                    assertTrue(f.delete());
                }
            }
        }
    }

    @Test
    void aggregate() throws IOException {
        assertFalse(aggregator.aggregate(false));
        writeToFile(jobDoneStreamDir + "/" + f1Data, 700 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(jobDoneStreamDir + "/" + f2Data, 200 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(jobDoneStreamDir + "/" + f3Data, 800 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(jobDoneStreamDir + "/" + f4Data, 900 * 1024);
        assertTrue(aggregator.aggregate(false));

        File[] files = new File(finalDir).listFiles();
        if (files != null) {
            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            assertEquals(1, finalDirFiles.size());
            assertEquals(contractNum + data1Ext, finalDirFiles.get(0).getName());
            assertEquals((900) * 1024, Files.size(Path.of(finalDirFiles.get(0).getAbsolutePath())));
        }

        assertFalse(aggregator.aggregate(false));

        File tmpFileDir = new File(getFileStreamingDirectory(jobId));
        assertTrue(tmpFileDir.delete());
        assertTrue(aggregator.aggregate(false));

        files = new File(finalDir).listFiles();
        if (files != null) {
            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            // "Finish" the job
            assertEquals(2, finalDirFiles.size());
            File f1 = finalDirFiles.get(0);
            File f2 = finalDirFiles.get(1);
            long size1 = Files.size(Path.of(f1.getAbsolutePath()));
            long size2 = Files.size(Path.of(f2.getAbsolutePath()));
            System.out.println(f1.getName() + ", " + size1);
            System.out.println(f2.getName() + ", " + size2);
            assertTrue((contractNum + data1Ext).equals(f1.getName()) || (contractNum + data2Ext).equals(f1.getName()));
            assertTrue((contractNum + data1Ext).equals(f2.getName()) || (contractNum + data2Ext).equals(f2.getName()));
            long size = Files.size(Path.of(f1.getAbsolutePath())) + Files.size(Path.of(f2.getAbsolutePath()));
            assertEquals((900 + 800 + 200) * 1024, size);

            assertTrue(aggregator.aggregate(false));
        }
        files = new File(finalDir).listFiles();
        if (files != null) {
            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            assertEquals(3, finalDirFiles.size());
            File f1 = finalDirFiles.get(0);
            File f2 = finalDirFiles.get(1);
            File f3 = finalDirFiles.get(2);
            long size = Files.size(Path.of(f1.getAbsolutePath())) + Files.size(Path.of(f2.getAbsolutePath())) + Files.size(Path.of(f3.getAbsolutePath()));
            assertEquals((700 + 900 + 800 + 200) * 1024, size);

            assertFalse(aggregator.aggregate(false));
        }

        files = new File(finalDir).listFiles();
        if (files != null) {

            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            assertEquals(3, finalDirFiles.size());
            File doneDir = new File(jobDoneStreamDir);
            files = doneDir.listFiles();
            assertNotNull(files);
            assertTrue(files.length == 0);
        }
    }

    @Test
    void getNextFileName() throws NoSuchFieldException, IllegalAccessException {
        assertEquals(finalDir + contractNum + data1Ext, aggregator.getNextFileName(false));
        assertEquals(finalDir + contractNum + data2Ext, aggregator.getNextFileName(false));
        assertEquals(finalDir + contractNum + "_0001_error.ndjson", aggregator.getNextFileName(true));

        // Reset the index so any other test won't have an invalid file index
        Field currentFileIndex = aggregator.getClass().getDeclaredField("currentFileIndex");
        currentFileIndex.setAccessible(true);
        currentFileIndex.setInt(aggregator, 1);
    }

    @Test
    void okayToDoAggregation() throws IOException {
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/" + f1Data, 700 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/" + f3Data, 200 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/" + f4Data, 800 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/f5.ndjson", 900 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/f6.ndjson", 1025 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/f7.ndjson", 101 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneStreamDir + "/f8.ndjson", 11 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
    }

    @Test
    void getBestFiles() throws IOException {
        writeToFile(jobDoneStreamDir + "/" + f1Data, 700 * 1024); // 2
        writeToFile(jobDoneStreamDir + "/" + f2Data, 20 * 1024); // 6
        writeToFile(jobDoneStreamDir + "/" + f3Data, 200 * 1024); // 3
        writeToFile(jobDoneStreamDir + "/" + f4Data, 800 * 1024); // 1
        writeToFile(jobDoneStreamDir + "/f5.ndjson", 100 * 1024); // 5
        writeToFile(jobDoneStreamDir + "/f6.ndjson", 1025 * 1024); // 0
        writeToFile(jobDoneStreamDir + "/f7.ndjson", 101 * 1024); // 4
        writeToFile(jobDoneStreamDir + "/f8.ndjson", 11 * 1024); // 7

        // The first file that is returned is the one that is too large
        List<File> bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals("f6.ndjson", bestFiles.get(0).getName());
        assertTrue(bestFiles.get(0).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(3, bestFiles.size());
        assertEquals(f4Data, bestFiles.get(0).getName());
        assertEquals(f3Data, bestFiles.get(1).getName());
        assertEquals(f2Data, bestFiles.get(2).getName());
        assertTrue(bestFiles.get(0).delete());
        assertTrue(bestFiles.get(1).delete());
        assertTrue(bestFiles.get(2).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(4, bestFiles.size());
        assertEquals(f1Data, bestFiles.get(0).getName());
        assertEquals("f7.ndjson", bestFiles.get(1).getName());
        assertEquals("f5.ndjson", bestFiles.get(2).getName());
        assertEquals("f8.ndjson", bestFiles.get(3).getName());
        assertTrue(bestFiles.get(0).delete());
        assertTrue(bestFiles.get(1).delete());
        assertTrue(bestFiles.get(2).delete());
        assertTrue(bestFiles.get(3).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(0, bestFiles.size());
    }

    @Test
    void getBestFilesBottomUp() throws IOException {
        assertTrue(aggregator.getBestFiles(false).isEmpty());
        writeToFile(jobDoneStreamDir + "/" + f1Data, 700);
        List<File> bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals(f1Data, bestFiles.get(0).getName());

        writeToFile(jobDoneStreamDir + "/" + f2Data, (1024 * 1024) + 10);
        bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals(f2Data, bestFiles.get(0).getName());
    }

    @Test
    void orderBySize() {
        List<FileDescriptor> sortedFdNull = aggregator.orderBySize(null);
        assertEquals(0, sortedFdNull.size());
        List<FileDescriptor> sortedFdEmpty = aggregator.orderBySize(Collections.emptyList());
        assertEquals(0, sortedFdEmpty.size());
        List<FileDescriptor> fd = List.of(
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f1.txt"), 1000),
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f2.txt"), 10),
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f3.txt"), 100),
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f4.txt"), 10000),
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f5.txt"), 100000),
                new FileDescriptor(new File(tmpDir.getAbsolutePath() + "/f6.txt"), 1)
        );
        List<FileDescriptor> sortedFd = aggregator.orderBySize(fd);
        assertEquals("f6.txt", sortedFd.get(0).getFile().getName());
        assertEquals("f2.txt", sortedFd.get(1).getFile().getName());
        assertEquals("f3.txt", sortedFd.get(2).getFile().getName());
        assertEquals("f1.txt", sortedFd.get(3).getFile().getName());
        assertEquals("f4.txt", sortedFd.get(4).getFile().getName());
        assertEquals("f5.txt", sortedFd.get(5).getFile().getName());
    }

    @Test
    void getSortedFileDescriptors() throws IOException {
        writeToFile(jobDoneStreamDir + "/" + f1Data, 100);
        writeToFile(jobDoneStreamDir + "/" + f2Data, 100000);
        writeToFile(jobDoneStreamDir + "/" + f3Data, 10000);
        writeToFile(jobDoneStreamDir + "/" + f4Data, 10);
        writeToFile(jobDoneStreamDir + "/f5.ndjson", 1);
        writeToFile(jobDoneStreamDir + "/f6.ndjson", 1000);
        writeToFile(jobDoneStreamDir + "/f7.ndjson", 101);
        writeToFile(jobDoneStreamDir + "/f8.ndjson", 11);
        writeToFile(jobDoneStreamDir + "/f1_error.ndjson", 99);
        writeToFile(jobDoneStreamDir + "/f2_error.ndjson", 9);

        List<FileDescriptor> fdsErrors = aggregator.getSortedFileDescriptors(true);
        assertEquals(2, fdsErrors.size());

        FileDescriptor f2Error = fdsErrors.get(0);
        assertEquals(9, f2Error.getSize());
        assertEquals("f2_error.ndjson", f2Error.getFile().getName());

        List<FileDescriptor> fds = aggregator.getSortedFileDescriptors(false);
        assertEquals(8, fds.size());

        FileDescriptor f5 = fds.get(0);
        assertEquals(1, f5.getSize());
        assertEquals("f5.ndjson", f5.getFile().getName());

        FileDescriptor f4 = fds.get(1);
        assertEquals(10, f4.getSize());
        assertEquals(f4Data, f4.getFile().getName());

        FileDescriptor f8 = fds.get(2);
        assertEquals(11, f8.getSize());
        assertEquals("f8.ndjson", f8.getFile().getName());

        FileDescriptor f1 = fds.get(3);
        assertEquals(100, f1.getSize());
        assertEquals(f1Data, f1.getFile().getName());

        FileDescriptor f7 = fds.get(4);
        assertEquals(101, f7.getSize());
        assertEquals("f7.ndjson", f7.getFile().getName());

        FileDescriptor f6 = fds.get(5);
        assertEquals(1000, f6.getSize());
        assertEquals("f6.ndjson", f6.getFile().getName());

        FileDescriptor f3 = fds.get(6);
        assertEquals(10000, f3.getSize());
        assertEquals(f3Data, f3.getFile().getName());

        FileDescriptor f2 = fds.get(7);
        assertEquals(100000, f2.getSize());
        assertEquals(f2Data, f2.getFile().getName());
    }

    static void writeToFile(String file, int numOfChars) throws IOException {
        String val = getAlphaNumericString(numOfChars);
        Files.write(Path.of(file), val.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    static String getAlphaNumericString(int n) {

        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = RANDOM.nextInt(alphaNumericString.length());
            sb.append(alphaNumericString.charAt(index));
        }

        return sb.toString();
    }
}