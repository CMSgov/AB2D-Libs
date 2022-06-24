package gov.cms.ab2d.eventclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.eventclient.clients.SQSConfig.EVENTS_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;

@SpringBootTest
@Testcontainers
public class SendSqsEventTest {

    static {
        System.setProperty("spring.liquibase.enabled", "false");
    }

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    private SQSEventClient SQSEventClient;

    @Autowired
    private AmazonSQS amazonSQS;

    @Autowired
    private ObjectMapper mapper;


    @Test
    void testQueueUrl() {
        String url = amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl();
        Assertions.assertTrue(url.contains("localhost:"));
        Assertions.assertTrue(url.contains(EVENTS_QUEUE));
    }

    @Test
    void testSendMessages() throws JsonProcessingException {
        AmazonSQS amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient = new SQSEventClient(amazonSQSSpy, mapper, true);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        SQSEventClient.send(sentApiRequestEvent);
        SQSEventClient.send(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl()).getMessages();
        List<Message> message2 = amazonSQS.receiveMessage(amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl()).getMessages();


        assertEquals(mapper.writeValueAsString(sentApiRequestEvent), message1.get(0).getBody());
        assertEquals(mapper.writeValueAsString(sentApiResponseEvent), message2.get(0).getBody());
    }
}
