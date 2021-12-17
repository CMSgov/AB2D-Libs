package gov.cms.ab2d.aggregator;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
/**
 * Loads the configuation for the aggregator. There are a few values:
 * 1. efs.mount - Location of the top level directory of the EFS mount to place files
 * 2. job.file.streaming - The location to place currently streaming files under the job directory
 * 3. job.file.finished - the location to place finished streaming files unde the job directory which
 *      lets the aggregator know it can combine these files
 * 4. job.aggregate.multiplier - the size multiplier to wait for before files are aggregator. This
 *      prevents the aggregator from getting too greedy. For example, we might want to wait for
 *      there to be 5 times the max file size before we start aggregating files
 * 5. job.file.rollover.ndjson - the max size of an aggregated file before it rolls over into another
 */
public final class ConfigManager {
    private final transient Properties prop = new Properties();
    private static ConfigManager instance;
    public static final int ONE_MEGA_BYTE = 1024 * 1024;

    private ConfigManager() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {

            //load a properties file from class path, inside static method
            prop.load(input);

        } catch (IOException ex) {
            log.error("Sorry, unable to find config.properties", ex);
        }
    }

    static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String getProperty(String key) {
        return getInstance().prop.getProperty(key);
    }
    public static int getMultiplier() {
        String mult = getInstance().getProperty("job.aggregate.multiplier");
        if (mult == null) {
            return 0;
        }
        return Integer.parseInt(mult);
    }

    public static String getJobDirectory(String jobId) {
        String dir = System.getenv("AB2D_EFS_MOUNT");
        if (dir == null || !dir.isEmpty()) {
            dir = getInstance().getProperty("efs.mount");
            String tmpDirLoc = "java.io.tmpdir";
            dir = dir.replace("${" + tmpDirLoc + "}", System.getProperty(tmpDirLoc));
        }
        return dir + "/" + jobId + "/";
    }

    public static String getFileStreamingDirectory(String jobId) {
        return getJobDirectory(jobId) + getInstance().getProperty("job.file.streaming") + "/";
    }

    public static String getFileDoneDirectory(String jobId) {
        return getJobDirectory(jobId) + getInstance().getProperty("job.file.finished") + "/";
    }

    public static long getMaxFileSize() {
        return Long.parseLong(getInstance().getProperty("job.file.rollover.ndjson")) * ONE_MEGA_BYTE;
    }
}
