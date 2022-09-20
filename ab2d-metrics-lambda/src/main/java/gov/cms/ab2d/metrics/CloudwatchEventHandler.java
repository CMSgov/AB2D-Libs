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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Optional;


// Catches cloudwatch alerts, extracts what we care about, then send an event to the ab2d-event sqs queue
public class CloudwatchEventHandler implements RequestHandler<SNSEvent, String> {
    private static AmazonSQS AMAZON_SQS;

    // AWS sends an object that's not wrapped with type info. The event service expects the wrapper.
    // Since there's not an easy way to enable/disable type wrapper just have 2 mappers.
    ObjectMapper inputMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    ObjectMapper outputMapper = new ObjectMapper()
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

    @SneakyThrows
    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        final LambdaLogger log = context.getLogger();
        log.log(inputMapper.writeValueAsString(snsEvent));
        snsEvent.getRecords()
                .forEach(record -> sendMetric(record, log));
        return "OK";
    }

    private void sendMetric(SNSEvent.SNSRecord record, LambdaLogger log) {
        SendMessageRequest request = new SendMessageRequest();
        String service;
        try {
            MetricAlarm alarm = inputMapper.readValue(Optional.ofNullable(record.getSNS())
                    .orElse(new SNSEvent.SNS())
                    .getMessage(), MetricAlarm.class);
            OffsetDateTime time = alarm.getStateChangeTime() != null
                    ? OffsetDateTime.parse(fixDate(alarm.getStateChangeTime()))
                    : OffsetDateTime.now();
            log.log(time.toString());
            request.setQueueUrl(AMAZON_SQS.getQueueUrl("ab2d-events")
                    .getQueueUrl());
            service = removeEnvironment(alarm.getAlarmName());
            request.setMessageBody(outputMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .timeOfEvent(time)
                    .stateType(String.valueOf(!alarm.getNewStateValue()
                            .equals(StateValue.OK.toString()) ? StateValue.ALARM : StateValue.OK))
                    .build())));
        } catch (Exception e) {
            log.log(String.format("Handling lambda failed %s", exceptionToString(e)));
            return;
        }
        AMAZON_SQS.sendMessage(request);

        log.log(String.format("Event %s sent to ab2d-events", service));
    }

    private String exceptionToString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }


    private String fixDate(String date) {
        StringBuilder builder = new StringBuilder(date);
        return builder.insert(builder.length() - 2, ":")
                .toString();
    }

    private String removeEnvironment(String alarmName) {
        return alarmName.replaceAll(Ab2dEnvironment.ALL.stream()
                .map(Ab2dEnvironment::getName)
                .filter(alarmName::contains)
                .findFirst()
                .orElse(""), "");

    }
}
