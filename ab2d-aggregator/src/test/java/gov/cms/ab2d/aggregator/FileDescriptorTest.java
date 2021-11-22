package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileDescriptorTest {

    @Test
    void getData() {
        FileDescriptor fd = new FileDescriptor(new File("/tmp"), 10);
        assertEquals("/tmp", fd.getFile().getAbsolutePath());
        assertEquals(10, fd.getSize());
    }

    @Test
    void getSize() {
    }
}