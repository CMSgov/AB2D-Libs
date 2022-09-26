package gov.cms.ab2d.eventclient.clients;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
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
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.support.NotificationMessageArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
@Slf4j
public class SQSConfig {

    private final String sqsQueueName;
    private static final String EVENTS_QUEUE = "-event-sqs";

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;

    private AWSStaticCredentialsProvider credentials;

    public SQSConfig(@Value("${cloud.aws.credentials.access-key}") String access,
                     @Value("${cloud.aws.credentials.secret-key}") String secret, @Value("${cloud.aws.region.static}") String region,
                     @Value("${cloud.aws.end-point.uri}") String url, Ab2dEnvironment ab2dEnvironment) {
        this.region = region;
        this.url = url;
        this.credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(access, secret));
        this.sqsQueueName = ab2dEnvironment.getName() + EVENTS_QUEUE;
    }


    @Primary
    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        log.info("Locakstack url " + url);
        if (null != url) {
            return (AmazonSQSAsync) createQueue(AmazonSQSAsyncClientBuilder
                    .standard()
                    .withEndpointConfiguration(getEndpointConfig(url))
                    .withCredentials(credentials)
                    .build());
        }
        return AmazonSQSAsyncClientBuilder
                .standard()
                .build();
    }

    @Bean
    public SQSEventClient sqsEventClient(AmazonSQS amazonSQS, @Value("${feature.sqs.enabled:false}") boolean enabled) {
        return new SQSEventClient(amazonSQS, objectMapper(), enabled, sqsQueueName);
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

    public String getSqsQueueName() {
        return sqsQueueName;
    }

}
