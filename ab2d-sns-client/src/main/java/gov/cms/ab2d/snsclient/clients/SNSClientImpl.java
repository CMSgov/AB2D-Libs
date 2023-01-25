package gov.cms.ab2d.snsclient.clients;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;

@Slf4j
public class SNSClientImpl implements SNSClient {
    private final AmazonSNSClient amazonSNSClient;
    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();

    private final Ab2dEnvironment ab2dEnvironment;

    public SNSClientImpl(AmazonSNSClient amazonSNSClient, Ab2dEnvironment ab2dEnvironment) {
        this.amazonSNSClient = amazonSNSClient;
        this.ab2dEnvironment = ab2dEnvironment;
    }


    @Override
    public void sendMessage(String topicName, Object message) throws JsonProcessingException {
        PublishRequest request = new PublishRequest();
        request.setTopicArn(amazonSNSClient.createTopic(ab2dEnvironment.getName() + "-" + topicName)
                .getTopicArn());
        request.setMessage(mapper.writeValueAsString(message));
        amazonSNSClient.publish(request);
    }
}
