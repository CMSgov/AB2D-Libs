package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.aggregator.FileOutputType.NDJSON;
import static gov.cms.ab2d.aggregator.FileOutputType.NDJSON_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileOutputTypeTest {

    @Test
    void isErrorFile() {
        String file1 = "abc." + NDJSON_ERROR.getSuffix();
        String file2 = "abc." + NDJSON.getSuffix();
        assertEquals(NDJSON_ERROR, FileOutputType.getFileType(file1));
        assertEquals(NDJSON, FileOutputType.getFileType(file2));
        assertNull(FileOutputType.getFileType("bogus.txt"));
        assertNull(FileOutputType.getFileType((String) null));
    }
}