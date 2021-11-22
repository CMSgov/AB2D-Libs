package gov.cms.ab2d.aggregator;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

/**
 * Holds the file and it's size. This enables us to manipulate ordering of files by only going to the file
 * system once
 */
@Getter
@AllArgsConstructor
public class FileDescriptor {
    private File file;
    private long size;
}
