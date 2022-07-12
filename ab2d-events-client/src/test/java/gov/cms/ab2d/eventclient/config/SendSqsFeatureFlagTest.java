package gov.cms.ab2d.eventclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;

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
}
