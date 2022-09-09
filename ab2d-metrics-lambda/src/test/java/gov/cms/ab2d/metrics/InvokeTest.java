package gov.cms.ab2d.metrics;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;

@Slf4j
class InvokeTest {

    @Test
    void invokeTest() {
        log.info("Invoke TEST");
        ScheduledEvent event = new ScheduledEvent();
        event.setDetail(Map.of("service", "api_efs"));
        Context context = new TestContext();
        CloudwatchEventHandler handler = new CloudwatchEventHandler();
        handler.handleRequest(event, context);
    }

}
