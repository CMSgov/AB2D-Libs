package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.support.NotificationMessageArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
@Slf4j
public class SQSConfig {

    private final String sqsQueueName;
    private static final String EVENTS_QUEUE = "-events-sqs";

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;

    public SQSConfig(@Value("${cloud.aws.region.static}") String region,
                     @Value("${cloud.aws.end-point.uri}") String url,
                     Ab2dEnvironment ab2dEnvironment) {
        this.region = region;
        this.url = url;
        this.sqsQueueName = ab2dEnvironment.getName() + EVENTS_QUEUE;

        // This is needed so the sqsListener can get the queue name.
        // It only accepts constance and this is a way to get around that while dynamically setting a sqs name
        System.setProperty("sqs.queue-name", sqsQueueName);
    }

    @Primary
    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        log.info("Locakstack url " + url);
        if (null != url) {
            return (AmazonSQSAsync) createQueue(AmazonSQSAsyncClientBuilder
                    .standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .withEndpointConfiguration(getEndpointConfig(url))
                    .build());
        }
        return AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    public SQSEventClient sqsEventClient(AmazonSQS amazonSQS) {
        return new SQSEventClient(amazonSQS, objectMapper(), sqsQueueName);
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(MessageConverter messageConverter) {
        var factory = new QueueMessageHandlerFactory();
        factory.setArgumentResolvers(List.of(new NotificationMessageArgumentResolver(messageConverter)));
        return factory;
    }

    /*
        This is a static method to avoid breaking existing projects mapping.
     */
    public static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        return objectMapper;
    }

    @Bean
    protected MessageConverter messageConverter() {
        MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
        jacksonMessageConverter.setObjectMapper(objectMapper());
        jacksonMessageConverter.setSerializedPayloadClass(String.class);
        jacksonMessageConverter.setStrictContentTypeMatch(false);
        return jacksonMessageConverter;
    }

    // Until localstack is built out more, create the queue here when running locally
    private AmazonSQS createQueue(AmazonSQS amazonSQS) {
        try {
            amazonSQS.getQueueUrl(sqsQueueName);
            log.info("Queue already exists");
        } catch (QueueDoesNotExistException e) {
            amazonSQS.createQueue(sqsQueueName);
            log.info("Queue created");
        }
        return amazonSQS;
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfig(String localstackURl) {
        return new AwsClientBuilder.EndpointConfiguration(localstackURl, region);
    }
}
