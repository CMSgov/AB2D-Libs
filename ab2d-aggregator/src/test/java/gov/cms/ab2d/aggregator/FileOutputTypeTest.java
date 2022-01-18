package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileOutputTypeTest {

    @Test
    void isErrorFile() {
        String file1 = "abc." + ERROR.getSuffix();
        String file2 = "abc." + DATA.getSuffix();
        assertEquals(ERROR, FileOutputType.getFileType(file1));
        assertEquals(DATA, FileOutputType.getFileType(file2));
        assertNull(FileOutputType.getFileType("bogus.txt"));
        assertNull(FileOutputType.getFileType((String) null));
    }
}