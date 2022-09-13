package gov.cms.ab2d.metrics;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


// Handler value: example.HandlerCWLogs
@Slf4j
public class CloudwatchEventHandler implements RequestHandler<MetricAlarm, String> {
    private static final AmazonSQS AMAZON_SQS;

    static {
        if (System.getenv("IS_LOCALSTACK") != null) {
            System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
            AMAZON_SQS = AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration("https://localhost:4566",
                            Regions.US_EAST_1.getName()))
                    .build();
        } else {
            AMAZON_SQS = AmazonSQSAsyncClientBuilder.standard()
                    .build();
        }

    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JodaModule())
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

    @Override
    public String handleRequest(MetricAlarm event, Context context) {

        String service = event.alarmName() + event.namespace();


        try {
            OffsetDateTime time = event.stateUpdatedTimestamp() != null ? event.stateUpdatedTimestamp()
                    .atOffset(ZoneOffset.of("America/New_York")) : OffsetDateTime.now();
            GetQueueUrlResult queue = AMAZON_SQS.getQueueUrl("ab2d-events");
            AMAZON_SQS.sendMessage(queue.getQueueUrl(), objectMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .timeOfEvent(time)
                    .stateType(String.valueOf(!event.stateValue()
                            .equals(StateValue.OK) ? StateValue.ALARM : StateValue.OK))
                    .build())
            ));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
        return "200 OK";
    }
}