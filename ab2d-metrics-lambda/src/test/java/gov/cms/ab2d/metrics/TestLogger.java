package gov.cms.ab2d.metrics;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestLogger implements LambdaLogger {

    public void log(String message) {
        log.info(message);
    }

    public void log(byte[] message) {
        log.info(new String(message));
    }
}
