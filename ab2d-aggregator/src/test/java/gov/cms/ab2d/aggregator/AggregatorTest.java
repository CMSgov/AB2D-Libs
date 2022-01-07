package gov.cms.ab2d.aggregator;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatorTest {

    private static final String JOB_ID = "job123";
    private static final String CONTRACT_NUM = "contractNum";
    private static final String F_1_NDJSON = "f1.ndjson";
    private static final String F_2_NDJSON = "f2.ndjson";
    private static final String F_3_NDJSON = "f3.ndjson";
    private static final String F_4_NDJSON = "f4.ndjson";
    private static final String F_5_NDJSON = "f5.ndjson";
    private static final String F_6_NDJSON = "f6.ndjson";
    private static final String F_7_NDJSON = "f7.ndjson";
    private static final String F_8_NDJSON = "f8.ndjson";
    private static final String DATA_1_EXT = "_0001.ndjson";
    private static final String DATA_2_EXT = "_0002.ndjson";
    private static final String FINISHED_DIR = "finished";
    private static final String STREAMING_DIR = "streaming";
    private static final int MAX_MEGA = 1;
    private static final int MULTIPLIER = 2;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void aggregate(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String finishedDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;
        assertFalse(aggregator.aggregate(false));
        writeToFile(finishedDir + "/" + F_1_NDJSON, 700 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(finishedDir + "/" + F_2_NDJSON, 200 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(finishedDir + "/" + F_3_NDJSON, 800 * 1024);
        assertFalse(aggregator.aggregate(false));
        writeToFile(finishedDir + "/" + F_4_NDJSON, 900 * 1024);
        assertTrue(aggregator.aggregate(false));

        String jobDir = tmpDir.getAbsolutePath() + "/" + JOB_ID;

        File[] files = new File(jobDir).listFiles();
        if (files != null) {
            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            assertEquals(1, finalDirFiles.size());
            assertEquals(CONTRACT_NUM + DATA_1_EXT, finalDirFiles.get(0).getName());
            assertEquals((900) * 1024, Files.size(Path.of(finalDirFiles.get(0).getAbsolutePath())));
        }

        assertFalse(aggregator.aggregate(false));

        File tmpFileDir = new File(tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + STREAMING_DIR);
        assertTrue(tmpFileDir.delete());
        assertTrue(aggregator.aggregate(false));

        files = new File(jobDir).listFiles();
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
            assertTrue((CONTRACT_NUM + DATA_1_EXT).equals(f1.getName()) || (CONTRACT_NUM + DATA_2_EXT).equals(f1.getName()));
            assertTrue((CONTRACT_NUM + DATA_1_EXT).equals(f2.getName()) || (CONTRACT_NUM + DATA_2_EXT).equals(f2.getName()));
            long size = Files.size(Path.of(f1.getAbsolutePath())) + Files.size(Path.of(f2.getAbsolutePath()));
            assertEquals((900 + 800 + 200) * 1024, size);

            assertTrue(aggregator.aggregate(false));
        }
        files = new File(jobDir).listFiles();
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

        files = new File(jobDir).listFiles();
        if (files != null) {

            List<File> finalDirFiles = Stream.of(files)
                    .filter(f -> !f.isDirectory())
                    .collect(Collectors.toList());
            assertEquals(3, finalDirFiles.size());
            File doneDir = new File(finishedDir);
            files = doneDir.listFiles();
            assertNotNull(files);
            assertEquals(0, files.length);
        }
    }

    @Test
    void getNextFileName(@TempDir File tmpDir) throws NoSuchFieldException, IllegalAccessException, IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String jobDir = tmpDir.getAbsolutePath() + "/" + JOB_ID;
        assertEquals(jobDir + "/" + CONTRACT_NUM + DATA_1_EXT, aggregator.getNextFileName(false));
        assertEquals(jobDir + "/" + CONTRACT_NUM + DATA_2_EXT, aggregator.getNextFileName(false));
        assertEquals(jobDir + "/" + CONTRACT_NUM + "_0001_error.ndjson", aggregator.getNextFileName(true));

        // Reset the index so any other test won't have an invalid file index
        Field currentFileIndex = aggregator.getClass().getDeclaredField("currentFileIndex");
        currentFileIndex.setAccessible(true);
        currentFileIndex.setInt(aggregator, 1);
    }

    @Test
    void okayToDoAggregation(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String jobDoneDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_1_NDJSON, 700 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_3_NDJSON, 200 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_4_NDJSON, 800 * 1024);
        assertFalse(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_5_NDJSON, 900 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_6_NDJSON, 1025 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_7_NDJSON, 101 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
        writeToFile(jobDoneDir + "/" + F_8_NDJSON, 11 * 1024);
        assertTrue(aggregator.okayToDoAggregation(false));
    }

    @Test
    void getBestFiles(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String jobDoneStreamDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;

        writeToFile(jobDoneStreamDir + "/" + F_1_NDJSON, 700 * 1024); // 2
        writeToFile(jobDoneStreamDir + "/" + F_2_NDJSON, 20 * 1024); // 6
        writeToFile(jobDoneStreamDir + "/" + F_3_NDJSON, 200 * 1024); // 3
        writeToFile(jobDoneStreamDir + "/" + F_4_NDJSON, 800 * 1024); // 1
        writeToFile(jobDoneStreamDir + "/" + F_5_NDJSON, 100 * 1024); // 5
        writeToFile(jobDoneStreamDir + "/" + F_6_NDJSON, 1025 * 1024); // 0
        writeToFile(jobDoneStreamDir + "/" + F_7_NDJSON, 101 * 1024); // 4
        writeToFile(jobDoneStreamDir + "/" + F_8_NDJSON, 11 * 1024); // 7

        // The first file that is returned is the one that is too large
        List<File> bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals(F_6_NDJSON, bestFiles.get(0).getName());
        assertTrue(bestFiles.get(0).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(3, bestFiles.size());
        assertEquals(F_4_NDJSON, bestFiles.get(0).getName());
        assertEquals(F_3_NDJSON, bestFiles.get(1).getName());
        assertEquals(F_2_NDJSON, bestFiles.get(2).getName());
        assertTrue(bestFiles.get(0).delete());
        assertTrue(bestFiles.get(1).delete());
        assertTrue(bestFiles.get(2).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(4, bestFiles.size());
        assertEquals(F_1_NDJSON, bestFiles.get(0).getName());
        assertEquals(F_7_NDJSON, bestFiles.get(1).getName());
        assertEquals(F_5_NDJSON, bestFiles.get(2).getName());
        assertEquals(F_8_NDJSON, bestFiles.get(3).getName());
        assertTrue(bestFiles.get(0).delete());
        assertTrue(bestFiles.get(1).delete());
        assertTrue(bestFiles.get(2).delete());
        assertTrue(bestFiles.get(3).delete());

        bestFiles = aggregator.getBestFiles(false);
        assertEquals(0, bestFiles.size());
    }

    @Test
    void getBestFilesBottomUp(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String jobDoneStreamDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;

        assertTrue(aggregator.getBestFiles(false).isEmpty());
        writeToFile(jobDoneStreamDir + "/" + F_1_NDJSON, 700);
        List<File> bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals(F_1_NDJSON, bestFiles.get(0).getName());

        writeToFile(jobDoneStreamDir + "/" + F_2_NDJSON, (1024 * 1024) + 10);
        bestFiles = aggregator.getBestFiles(false);
        assertEquals(1, bestFiles.size());
        assertEquals(F_2_NDJSON, bestFiles.get(0).getName());
    }

    @Test
    void orderBySize(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
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
    void getSortedFileDescriptors(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        String jobDoneStreamDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;
        writeToFile(jobDoneStreamDir + "/" + F_1_NDJSON, 100);
        writeToFile(jobDoneStreamDir + "/" + F_2_NDJSON, 100000);
        writeToFile(jobDoneStreamDir + "/" + F_3_NDJSON, 10000);
        writeToFile(jobDoneStreamDir + "/" + F_4_NDJSON, 10);
        writeToFile(jobDoneStreamDir + "/" + F_5_NDJSON, 1);
        writeToFile(jobDoneStreamDir + "/" + F_6_NDJSON, 1000);
        writeToFile(jobDoneStreamDir + "/" + F_7_NDJSON, 101);
        writeToFile(jobDoneStreamDir + "/" + F_8_NDJSON, 11);
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
        assertEquals(F_5_NDJSON, f5.getFile().getName());

        FileDescriptor f4 = fds.get(1);
        assertEquals(10, f4.getSize());
        assertEquals(F_4_NDJSON, f4.getFile().getName());

        FileDescriptor f8 = fds.get(2);
        assertEquals(11, f8.getSize());
        assertEquals(F_8_NDJSON, f8.getFile().getName());

        FileDescriptor f1 = fds.get(3);
        assertEquals(100, f1.getSize());
        assertEquals(F_1_NDJSON, f1.getFile().getName());

        FileDescriptor f7 = fds.get(4);
        assertEquals(101, f7.getSize());
        assertEquals(F_7_NDJSON, f7.getFile().getName());

        FileDescriptor f6 = fds.get(5);
        assertEquals(1000, f6.getSize());
        assertEquals(F_6_NDJSON, f6.getFile().getName());

        FileDescriptor f3 = fds.get(6);
        assertEquals(10000, f3.getSize());
        assertEquals(F_3_NDJSON, f3.getFile().getName());

        FileDescriptor f2 = fds.get(7);
        assertEquals(100000, f2.getSize());
        assertEquals(F_2_NDJSON, f2.getFile().getName());
    }

    @Test
    void testIsJobAggregated(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);

        assertFalse(aggregator.isJobAggregated());

        JobHelper.workerFinishJob(tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + STREAMING_DIR);

        aggregator.aggregate(false);

        assertFalse(aggregator.isJobAggregated());

        JobHelper.aggregatorFinishJob(tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR);

        aggregator.aggregate(true);

        assertTrue(aggregator.isJobAggregated());
    }

    @Test
    void testRemoveEmptyFiles(@TempDir File tmpDir) throws IOException {
        Aggregator aggregator = new Aggregator(JOB_ID, CONTRACT_NUM, tmpDir.getAbsolutePath(), MAX_MEGA, STREAMING_DIR,
                FINISHED_DIR, MULTIPLIER);
        aggregator.removeEmptyFiles();
        String jobDoneStreamDir = tmpDir.getAbsolutePath() + "/" + JOB_ID + "/" + FINISHED_DIR;
        writeToFile(jobDoneStreamDir + "/" + F_1_NDJSON, 0);
        File f1 = Path.of(jobDoneStreamDir + "/" + F_1_NDJSON).toFile();
        assertTrue(f1.exists());
        aggregator.removeEmptyFiles();
        assertFalse(f1.exists());
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