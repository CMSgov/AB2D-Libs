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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static gov.cms.ab2d.eventclient.events.MetricsEvent.State.END;
import static gov.cms.ab2d.eventclient.events.MetricsEvent.State.START;
import static software.amazon.awssdk.services.cloudwatch.model.StateValue.ALARM;
import static software.amazon.awssdk.services.cloudwatch.model.StateValue.OK;


// Catches cloudwatch alerts, extracts what we care about, then send an event to the ab2d-event sqs queue
public class CloudwatchEventHandler implements RequestHandler<SNSEvent, String> {
    private static AmazonSQS amazonSQS;

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
        amazonSQS = setup();
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
        log.log(snsEvent.toString());
        snsEvent.getRecords()
                .forEach(snsRecord -> sendMetric(snsRecord, log));
        return "OK";
    }

    private void sendMetric(SNSEvent.SNSRecord snsRecord, LambdaLogger log) {
        SendMessageRequest request = new SendMessageRequest();
        String service;
        try {
            MetricAlarm alarm = inputMapper.readValue(Optional.ofNullable(snsRecord.getSNS())
                    .orElse(new SNSEvent.SNS())
                    .getMessage(), MetricAlarm.class);
            OffsetDateTime time = alarm.getStateChangeTime() != null
                    ? OffsetDateTime.parse(fixDate(alarm.getStateChangeTime()))
                    : OffsetDateTime.now();
            log.log(time.toString());
            request.setQueueUrl(amazonSQS.getQueueUrl("ab2d-events")
                    .getQueueUrl());
            service = removeEnvironment(alarm.getAlarmName());
            request.setMessageBody(outputMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .eventDescription(alarm.getAlarmDescription())
                    .timeOfEvent(time)
                    //This might need more work later if AWS is sending unknown states regularly
                    .stateType(from(alarm.getNewStateValue()))
                    .build())));
        } catch (Exception e) {
            log.log(String.format("Handling lambda failed %s", exceptionToString(e)));
            return;
        }
        amazonSQS.sendMessage(request);

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
        if (alarmName == null) {
            throw new RuntimeException("Service was not defined");
        }
        return cleanUpService(alarmName.replaceAll(Ab2dEnvironment.ALL.stream()
                .map(Ab2dEnvironment::getName)
                .filter(alarmName::contains)
                .findFirst()
                .orElse(""), ""));
    }

    private String cleanUpService(String service) {
        return service.length() > 1 && service.charAt(0) == '-'
                ? service.substring(1)
                : service; // clean up service
    }

    private MetricsEvent.State from(String stateValue) {
        return Stream.of(stateValue)
                .filter(value1 -> List.of(OK.toString(), ALARM.toString())
                        .contains(value1))
                .map(state -> stateValue.equals(OK.toString()) ? END : START)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("AWS provided Unknown State %s", stateValue)));
    }
}
