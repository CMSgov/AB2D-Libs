package gov.cms.ab2d.metrics;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

import java.time.OffsetDateTime;
import java.util.Optional;


// Catches cloudwatch alerts, extracts what we care about, then send an event to the ab2d-event sqs queue
public class CloudwatchEventHandler implements RequestHandler<SNSEvent, String> {
    private static AmazonSQS AMAZON_SQS;

    private final Gson gson = new Gson();
    // For whatever reason jackson refuses to deserialize MetricAlarm yet gson handles it without issue
    // We still need objectmapper since event-service can't handle GeneralSQSMessage serialized by gson.
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    static {
        AMAZON_SQS = setup();
    }

    private static AmazonSQS setup() {
        if (!StringUtils.isNullOrEmpty(System.getenv("IS_LOCALSTACK"))) {
            System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration("https://localhost:4566",
                            Regions.US_EAST_1.getName()))
                    .build();
        } else {
            return AmazonSQSAsyncClientBuilder.standard()
                    .build();
        }
    }

    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        final LambdaLogger log = context.getLogger();
        snsEvent.getRecords()
                .forEach(record -> sendMetric(record, log));
        return "OK";
    }

    private void sendMetric(SNSEvent.SNSRecord record, LambdaLogger log) {
        MetricAlarm alarm = gson.fromJson(Optional.ofNullable(record.getSNS())
                .orElse(new SNSEvent.SNS())
                .getMessage(), MetricAlarm.class);

        String service = alarm.getAlarmName() + alarm.getNamespace();
        OffsetDateTime time = alarm.getStateChangeTime() != null
                ? OffsetDateTime.parse(fixDate(alarm.getStateChangeTime()))
                : OffsetDateTime.now();
        SendMessageRequest request = new SendMessageRequest();
        request.setQueueUrl(AMAZON_SQS.getQueueUrl("ab2d-events")
                .getQueueUrl());
        try {
            request.setMessageBody(objectMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .timeOfEvent(time)
                    .stateType(String.valueOf(!alarm.getNewStateValue()
                            .equals(StateValue.OK.toString()) ? StateValue.ALARM : StateValue.OK))
                    .build())));
        } catch (Exception e) {
            log.log(String.format("Handling lambda failed %s", e));
            return;
        }
        AMAZON_SQS.sendMessage(request);

        log.log(String.format("Event do %s sent to ab2d-events", service));
    }


    private String fixDate(String date) {
        StringBuilder builder = new StringBuilder(date);
        return builder.insert(builder.length() - 2, ":")
                .toString();
    }
}
