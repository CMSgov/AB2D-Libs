package gov.cms.ab2d.metrics;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

// Handler value: example.HandlerCWLogs
@Slf4j
public class CloudwatchEventHandler implements RequestHandler<ScheduledEvent, String> {
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
    public String handleRequest(ScheduledEvent event, Context context) {

        String service = Optional.ofNullable(event.getDetail())
                .filter(Objects::nonNull)
                .map(detail -> detail.get("service"))
                .stream()
                .findFirst()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Invalid service"));

        try {
            OffsetDateTime time = event.getTime() != null ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.getTime()
                    .getMillis()), ZoneId.of(event.getTime()
                    .getZone()
                    .getID())) : OffsetDateTime.now();
            GetQueueUrlResult queue = AMAZON_SQS.getQueueUrl("ab2d-events");
            AMAZON_SQS.sendMessage(queue.getQueueUrl(), objectMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .timeOfEvent(time)
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
