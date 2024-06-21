package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.aggregator.FileOutputType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

class FileOutputTypeTest {

    @Test
    void isErrorFile() {
        String file1 = "abc." + ERROR.getSuffix();
        String file2 = "abc." + DATA.getSuffix();
        assertEquals(ERROR, FileOutputType.getFileType(file1));
        assertEquals(DATA, FileOutputType.getFileType(file2));
        assertEquals(UNKNOWN, FileOutputType.getFileType("bogus.txt"));
        assertEquals(UNKNOWN, FileOutputType.getFileType((String) null));
    }

    @Test
    void testActualFile() {
        File file1 = new File("abc." + ERROR.getSuffix());
        File file2 = new File("abc." + DATA.getSuffix());
        assertEquals(ERROR, FileOutputType.getFileType(file1));
        assertEquals(DATA, FileOutputType.getFileType(file2));
    }
}
