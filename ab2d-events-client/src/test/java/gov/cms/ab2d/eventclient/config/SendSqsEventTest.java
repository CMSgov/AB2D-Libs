package gov.cms.ab2d.eventclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import java.util.ArrayList;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
public class SendSqsEventTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();
    public static final String LOCAL_EVENTS_SQS = "local-events-sqs";
    public static final String DEV_EVENTS_SQS = "dev-events-sqs";

    @Autowired
    private AmazonSQS amazonSQS;

    private final ObjectMapper mapper = SQSConfig.objectMapper();

    @Test
    void testQueueUrl() {
        String url = amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl();
        assertTrue(url.contains(LOCAL_EVENTS_SQS));
    }

    @Test
    void testQueueUrlNoExistingQueue() {
        Assertions.assertThrows(QueueDoesNotExistException.class, () -> {
            amazonSQS.getQueueUrl(DEV_EVENTS_SQS);
        });
    }

    @Test
    void testSendMessages() throws JsonProcessingException {
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, LOCAL_EVENTS_SQS);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        List<Message> message2 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();

        assertTrue(message1.get(0).getBody().contains(mapper.writeValueAsString(sentApiRequestEvent)));
        assertTrue(message2.get(0).getBody().contains(mapper.writeValueAsString(sentApiResponseEvent)));
    }

    @Test
    void testSendMessagesDifferentQueue() throws JsonProcessingException {
        amazonSQS.createQueue("ab2d-dev-events-sqs");
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, "ab2d-dev-events-sqs");

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl("ab2d-dev-events-sqs").getQueueUrl()).getMessages();
        List<Message> message2 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl("ab2d-dev-events-sqs").getQueueUrl()).getMessages();

        assertTrue(message1.get(0).getBody().contains(mapper.writeValueAsString(sentApiRequestEvent)));
        assertTrue(message2.get(0).getBody().contains(mapper.writeValueAsString(sentApiResponseEvent)));
    }

    @Test
    void logWithSQS() {
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, LOCAL_EVENTS_SQS);

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

        Mockito.verify(amazonSQSSpy, timeout(1000).times(7)).sendMessage(any(SendMessageRequest.class));

        List<Message> message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("GeneralSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("TraceSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("AlertSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("SlackSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("KinesisSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("TraceAndAlertSQSMessage"));

        message = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS).getQueueUrl()).getMessages();
        assertTrue(message.get(0).getBody().contains("LogAndTraceSQSMessage"));
    }

    @Test
    void testFailedMapping(CapturedOutput output) {
        AmazonSQS amazonSQSMock = Mockito.mock(AmazonSQS.class);
        GetQueueUrlResult queueURL = Mockito.mock(GetQueueUrlResult.class);

        when(amazonSQSMock.getQueueUrl(anyString())).thenReturn(queueURL);
        when(queueURL.getQueueUrl()).thenReturn("localhost:4321");
        when(amazonSQSMock.sendMessage(any(SendMessageRequest.class))).thenThrow(new UnsupportedOperationException("foobar"));
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSMock, mapper, LOCAL_EVENTS_SQS);

        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        Assertions.assertTrue(output.getOut().contains("foobar"));
    }

    @Test
    void testAB2DEEnvironment() {
        new SQSConfig("", "", Ab2dEnvironment.DEV);
        assertEquals("ab2d-dev-events-sqs", System.getProperty("sqs.queue-name"));

        new SQSConfig("", "", Ab2dEnvironment.SANDBOX);
        assertEquals("ab2d-sbx-sandbox-events-sqs", System.getProperty("sqs.queue-name"));
    }
}
