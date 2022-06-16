package gov.cms.ab2d.eventlibs.events;


import gov.cms.ab2d.eventlibs.utils.UtilMethods;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Log events that happen to files such as when they are open, closed or deleted
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class FileEvent extends LoggableEvent {
    public enum FileStatus {
        OPEN,
        CLOSE,
        DELETE
    }
    // The file name involved
    private String fileName;
    // The new of the file
    private FileStatus status;
    // The size of the file if we're closing it
    private long fileSize;
    // The content hash so we can compare different files
    private String fileHash;

    public FileEvent() { }

    public FileEvent(String organization, String jobId, File file, FileStatus status) {
        super(OffsetDateTime.now(), organization, jobId);
        this.status = status;
        if (file != null) {
            this.fileName = file.getAbsolutePath();
            this.fileSize = file.length();
            this.fileHash = generateChecksum(file);
        }
    }

    private String generateChecksum(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return UtilMethods.hashIt(fileInputStream);
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public String asMessage() {
        return String.format("(%s): %s %s", getJobId(), status, fileName);
    }
}
