package gov.cms.ab2d.aggregator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * This manages all the creating and streaming of beneficiary data. To use this, you create a try
 * with resources with the BeneficiaryStream. For example:
 *
 * try (BeneficiaryStream stream = new BeneficiaryStream(jobId, false)) {
 *     for (int i = 0; i < beneBatchSize; i++) {
 *         stream.write(getNdJson(benes.get(i)));
 *     }
 * } catch(Exception ex) { }
 *
 * This does all the work of creating a temporary file in the correct location to stream, allowing
 * streams to be written to and when the file is closed, it moves it to the "done" directory, waiting
 * for the aggregator to pick it up
 */
public class BeneficiaryStream implements AutoCloseable {
    private static final String FILE_PREFIX = "tmp_";
    private final transient BufferedOutputStream bout;
    private final transient File tmpFile;
    private final transient File completeFile;
    private final transient String jobId;
    private final transient boolean error;
    private transient boolean open = false;
    private final transient FileOutputStream stream;

    BeneficiaryStream(String jobId, boolean error) throws IOException {
        this.jobId = jobId;
        this.error = error;
        this.open = true;
        JobHelper.workerSetUpJobDirectories(jobId);
        tmpFile = createNewFile();
        stream = new FileOutputStream(tmpFile);
        bout = new BufferedOutputStream(stream);
        File directory = new File(ConfigManager.getFileDoneDirectory(this.jobId));
        String file = tmpFile.getName();
        completeFile = Path.of(directory.getAbsolutePath(), file).toFile();
    }

    @Override
    public void close() throws IOException {
        bout.flush();
        bout.close();
        if (stream != null) {
            stream.close();
        }
        this.open = false;
        moveFileToDone();
    }

    public File getFile() {
        if (this.open) {
            return tmpFile;
        } else {
            return completeFile;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    boolean moveFileToDone() {
        return tmpFile.renameTo(completeFile);
    }

    void write(String val) throws IOException {
        bout.write(val.getBytes(StandardCharsets.UTF_8));
    }

    void flush() throws IOException {
        bout.flush();
    }

    private File createNewFile() throws IOException {
        String suffix = this.error ? FileOutputType.NDJSON_ERROR.getSuffix() : FileOutputType.NDJSON.getSuffix();
        File directory = new File(ConfigManager.getFileStreamingDirectory(this.jobId));
        return File.createTempFile(FILE_PREFIX, suffix, directory);
    }

}
