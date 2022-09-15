package gov.cms.ab2d.eventclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = { "spring.liquibase.enabled=false", "feature.sqs.enabled=false"})
@Testcontainers
public class SendSqsFeatureFlagTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    private SQSEventClient sqsEventClient;

    @Mock
    private AmazonSQS amazonSQS;

    @Test
    void testSendMessagesFlagDisabled() throws JsonProcessingException {

        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQS, timeout(1000).times(0)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void logWithSQS() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");

        ArrayList<Ab2dEnvironment> enviroments = new ArrayList<>();
        enviroments.add(Ab2dEnvironment.LOCAL);
        sqsEventClient.sendLogs(event);
        sqsEventClient.trace(event.getDescription(), enviroments);
        sqsEventClient.alert(event.getDescription(), enviroments);
        sqsEventClient.log(EventClient.LogType.SQL, event);
        sqsEventClient.log(EventClient.LogType.KINESIS, event);
        sqsEventClient.logAndAlert(event, enviroments);
        sqsEventClient.logAndTrace(event, enviroments);

        Mockito.verify(amazonSQS, timeout(1000).times(0)).sendMessage(any(SendMessageRequest.class));

    }
}
