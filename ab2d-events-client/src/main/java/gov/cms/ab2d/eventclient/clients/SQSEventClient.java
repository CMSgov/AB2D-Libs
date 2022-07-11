package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import lombok.extern.slf4j.Slf4j;

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
    public void sendLogs(LoggableEvent requestEvent) {
        if (enabled) {
            String queueUrl = amazonSQS.getQueueUrl(EVENTS_QUEUE).getQueueUrl();

            GeneralSQSMessage message = new GeneralSQSMessage(requestEvent);
            try {
                SendMessageRequest sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(mapper.writeValueAsString(message));

                amazonSQS.sendMessage(sendMessageRequest);
            } catch (JsonProcessingException | UnsupportedOperationException | InvalidMessageContentsException e) {
                log.info(e.getMessage());
            }
        }
    }
}
