package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import static gov.cms.ab2d.eventclient.clients.SQSConfig.EVENTS_QUEUE;

@Slf4j
public class SQSEventClient implements EventClient {
    private final AmazonSQS amazonSQS;
    private final ObjectMapper mapper;

    private final boolean enabled;

    public SQSEventClient(AmazonSQS amazonSQS, ObjectMapper mapper, boolean enabled) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    @Override
    public void send(LoggableEvent requestEvent) {
        if (enabled) {
            String queueUrl = amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl();

            SendMessageRequest sendMessageRequest = null;
            try {
                sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(mapper.writeValueAsString(requestEvent));
            } catch (JsonProcessingException e) {
                log.info("error mapping event");
            }
            try {
                amazonSQS.sendMessage(sendMessageRequest);
            } catch (UnsupportedOperationException | InvalidMessageContentsException e) {
                log.info(e.getMessage());
            }
        }
    }
}
