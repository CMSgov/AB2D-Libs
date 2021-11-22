package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static gov.cms.ab2d.aggregator.ConfigManager.ONE_MEGA_BYTE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AggregatorCallableTest {
    private static final String JOB_ID = "jobby";
    private static final String FILE_1 = "file1.txt";
    private final transient  ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    @AfterEach
    void cleanUp() {
        List<File> allFiles = new ArrayList<>();
        File[] jobDoneDirFiles = new File(ConfigManager.getFileDoneDirectory(JOB_ID)).listFiles();
        if (jobDoneDirFiles != null) {
            allFiles.addAll(List.of(jobDoneDirFiles));
        }
        File[] jobStreamDirFiles = new File(ConfigManager.getFileStreamingDirectory(JOB_ID)).listFiles();
        if (jobStreamDirFiles != null) {
            allFiles.addAll(List.of(jobStreamDirFiles));
        }
        File[] jobDirFiles = new File(ConfigManager.getJobDirectory(JOB_ID)).listFiles();
        if (jobDirFiles != null) {
            allFiles.addAll(List.of(jobDirFiles));
        }
        for (File f : allFiles) {
            assertTrue(f.delete());
        }
    }

    @Test
    void doItAll() throws IOException, InterruptedException {
        long t1 = System.currentTimeMillis();
        AggregatorCallable callable = new AggregatorCallable(this.JOB_ID, "contract");
        JobHelper.workerSetUpJobDirectories(this.JOB_ID);
        Future<Integer> future = executor.submit(callable);
        // For each batch
        for (int i = 0; i < 100; i++) {
            // Create a file for the batch of beneficiaries
            try (BeneficiaryStream stream = new BeneficiaryStream(JOB_ID, false)) {
                // For each beneficiary
                for (int b = 0; b < 250; b++) {
                    int length = RANDOM.nextInt(1000);
                    stream.write(AggregatorTest.getAlphaNumericString(length) + "\n");
                }
            } catch (Exception ex) {
                fail(ex);
            }
        }
        JobHelper.workerFinishJob(this.JOB_ID);
        while (!future.isDone()) {
            Thread.sleep(1000);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Time is: " + (t2 - t1) / 1000);
    }

    @Disabled
    @Test
    void combineBigFiles() throws IOException {
        String jobDir = ConfigManager.getJobDirectory(JOB_ID);

        System.out.println("Creating files");
        AggregatorTest.writeToFile(jobDir + FILE_1, 100 * ONE_MEGA_BYTE);
        System.out.println("Now copy first file");
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file2.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file3.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file4.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file5.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file6.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file7.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file8.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file9.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file10.txt"));
        System.out.println("Done creating files");
        long t1 = System.currentTimeMillis();
        FileUtils.combineFiles(List.of(new File(jobDir + "file1.txt"),
                        new File(jobDir + "file2.txt"),
                        new File(jobDir + "file3.txt"),
                        new File(jobDir + "file4.txt"),
                        new File(jobDir + "file5.txt"),
                        new File(jobDir + "file6.txt"),
                        new File(jobDir + "file7.txt"),
                        new File(jobDir + "file8.txt"),
                        new File(jobDir + "file9.txt"),
                        new File(jobDir + "file10.txt")
                ),
                jobDir + "outfile.txt");
        long t2 = System.currentTimeMillis();
        System.out.println("Combining files to (" + (t2 - t1) + ")");
    }
}