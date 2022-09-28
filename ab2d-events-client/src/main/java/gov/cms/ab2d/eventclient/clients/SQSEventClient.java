package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.messages.AlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.KinesisSQSMessage;
import gov.cms.ab2d.eventclient.messages.LogAndTraceSQSMessage;
import gov.cms.ab2d.eventclient.messages.SQSMessages;
import gov.cms.ab2d.eventclient.messages.SlackSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceSQSMessage;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQSEventClient implements EventClient {
    private final AmazonSQS amazonSQS;
    private final ObjectMapper mapper;
    private final boolean enabled;

    private final String queueName;

    public SQSEventClient(AmazonSQS amazonSQS, ObjectMapper mapper, boolean enabled, String queueName) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper;
        this.enabled = enabled;
        this.queueName = queueName;
    }

    @Override
    public void sendLogs(LoggableEvent requestEvent) {
        if (enabled) {
            GeneralSQSMessage sqsMessage = new GeneralSQSMessage(requestEvent);
            sendMessage(sqsMessage);
        }
    }

    @Override
    public void alert(String message, List<Ab2dEnvironment> environments) {
        if (enabled) {
            AlertSQSMessage sqsMessage = new AlertSQSMessage(message, environments);
            sendMessage(sqsMessage);
        }
    }

    /**
     * Alert only AB2D team via relevant loggers with a low priority alert
     *
     * @param message      message to provide
     * @param environments AB2D environments to alert on
     */
    @Override
    public void trace(String message, List<Ab2dEnvironment> environments) {
        if (enabled) {
            TraceSQSMessage sqsMessage = new TraceSQSMessage(message, environments);
            sendMessage(sqsMessage);
        }
    }

    /**
     * Log the event and alert to relevant alert loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    @Override
    public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
        if (enabled) {
            TraceAndAlertSQSMessage sqsMessage = new TraceAndAlertSQSMessage(event, environments);
            sendMessage(sqsMessage);
        }
    }

    /**
     * Log the event and provided traces to relevant trace loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    @Override
    public void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments) {
        if (enabled) {
            LogAndTraceSQSMessage sqsMessage = new LogAndTraceSQSMessage(event, environments);
            sendMessage(sqsMessage);
        }
    }

    /**
     * Log an event without alerting
     *
     * @param type  type of event logger to use
     * @param event event to log
     */
    @Override
    public void log(LogType type, LoggableEvent event) {
        if (enabled) {
            switch (type) {
                case SQL:
                    sendMessage(new SlackSQSMessage(event));
                    break;
                case KINESIS:
                    sendMessage(new KinesisSQSMessage(event));
                    break;
                default:
                    break;
            }
        }
    }

    private void sendMessage(SQSMessages message) {
        String queueUrl = amazonSQS.getQueueUrl(queueName).getQueueUrl();
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
