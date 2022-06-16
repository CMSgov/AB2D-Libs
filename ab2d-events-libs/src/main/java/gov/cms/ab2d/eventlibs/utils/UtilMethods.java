package gov.cms.ab2d.eventlibs.utils;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public final class UtilMethods {

    private UtilMethods() { }

    public static String hashIt(String val) {
        if (val == null) {
            return null;
        }
        return Hex.encodeHexString(DigestUtils.sha256(val));
    }

    public static String hashIt(InputStream stream) throws IOException {
        return Hex.encodeHexString(DigestUtils.sha256(stream));
    }
}
