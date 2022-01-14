package gov.cms.ab2d.aggregator;

import java.io.File;

/**
 * Taken from AB2D. Describes the different file endings for the data and error files created by the aggregator
 */
public enum FileOutputType {
    NDJSON(".ndjson"),
    NDJSON_ERROR("_error.ndjson"),
    UNKNOWN("");

    private final String suffix;

    FileOutputType(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public static FileOutputType getFileType(File file) {
        return getFileType(file.getAbsolutePath());
    }

    public static FileOutputType getFileType(String file) {
        if (file == null) {
            return UNKNOWN;
        }
        if (file.endsWith(NDJSON.getSuffix())) {
            if (file.endsWith(NDJSON_ERROR.getSuffix())) {
                return NDJSON_ERROR;
            }
            return NDJSON;
        }
        return UNKNOWN;
    }
}