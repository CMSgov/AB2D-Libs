package gov.cms.ab2d.snsclient.clients;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SNSClientImpl implements SNSClient {
    private final AmazonSNSClient amazonSNSClient;
    private final ObjectMapper mapper;

    private final Ab2dEnvironment ab2dEnvironment;

    public SNSClientImpl(AmazonSNSClient amazonSNSClient, ObjectMapper mapper, Ab2dEnvironment ab2dEnvironment) {
        this.amazonSNSClient = amazonSNSClient;
        this.mapper = mapper;
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
