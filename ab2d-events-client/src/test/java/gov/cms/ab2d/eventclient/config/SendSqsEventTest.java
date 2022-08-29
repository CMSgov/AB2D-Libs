package gov.cms.ab2d.eventclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.eventclient.clients.SQSConfig.EVENTS_QUEUE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = { "spring.liquibase.enabled=false"})
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
public class SendSqsEventTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    private AmazonSQS amazonSQS;

    private final ObjectMapper mapper = SQSConfig.objectMapper();


    @Test
    void testQueueUrl() {
        String url = amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl();
        assertTrue(url.contains("localhost:"));
        assertTrue(url.contains(EVENTS_QUEUE));
    }

    @Test
    void testSendMessages() throws JsonProcessingException {
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, true);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl()).getMessages();
        List<Message> message2 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl()).getMessages();


        assertTrue(message1.get(0).getBody().contains(mapper.writeValueAsString(sentApiRequestEvent)));
        assertTrue(message2.get(0).getBody().contains(mapper.writeValueAsString(sentApiResponseEvent)));

    }

    @Test
    void testFailedMapping(CapturedOutput output) {
        AmazonSQS amazonSQSMock = Mockito.mock(AmazonSQS.class);
        GetQueueUrlResult queueURL = Mockito.mock(GetQueueUrlResult.class);

        when(amazonSQSMock.getQueueUrl(anyString())).thenReturn(queueURL);
        when(queueURL.getQueueUrl()).thenReturn("localhost:4321");
        when(amazonSQSMock.sendMessage(any(SendMessageRequest.class))).thenThrow(new UnsupportedOperationException("foobar"));
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSMock, mapper, true);

        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        Assertions.assertTrue(output.getOut().contains("foobar"));
    }

    @Test
    void testSendMessagesWhenDisabled() {
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, false);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);

        Mockito.verify(amazonSQSSpy, never()).sendMessage(any(SendMessageRequest.class));
    }
}
