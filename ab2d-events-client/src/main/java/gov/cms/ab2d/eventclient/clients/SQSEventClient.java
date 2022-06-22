package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import static gov.cms.ab2d.eventclient.clients.SQSConfig.EVENTS_QUEUE;

@Slf4j
@Component
public class SQSEventClient implements EventClient {
    private AmazonSQS amazonSQS;
    private ObjectMapper mapper;

    public SQSEventClient(AmazonSQS amazonSQS, ObjectMapper mapper) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper;
    }

    @Override
    public void send(LoggableEvent requestEvent) throws JsonProcessingException {
        String queueUrl = amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl();

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(mapper.writeValueAsString(requestEvent));
        amazonSQS.sendMessage(sendMessageRequest);
    }
}
