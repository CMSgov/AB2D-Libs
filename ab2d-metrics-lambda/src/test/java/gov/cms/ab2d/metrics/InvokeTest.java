package gov.cms.ab2d.metrics;

import com.amazonaws.services.lambda.runtime.Context;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

@Slf4j
class InvokeTest {

    @Test
    void invokeTest() {
        log.info("Invoke TEST");
        MetricAlarm event = MetricAlarm.builder()
                .alarmName("ab2d-east-impl-healthy-host")
                .namespace("AWS/ApplicationELB")
                .stateValue(StateValue.ALARM)
                .build();
        Context context = new TestContext();
        CloudwatchEventHandler handler = new CloudwatchEventHandler();
        handler.handleRequest(event, context);
    }

}
