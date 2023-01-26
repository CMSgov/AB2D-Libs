package gov.cms.ab2d.snsclient.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.snsclient.clients.SNSClient;
import gov.cms.ab2d.snsclient.clients.SNSClientImpl;
import gov.cms.ab2d.snsclient.clients.SNSConfig;
import gov.cms.ab2d.snsclient.messages.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest//(classes = {SNSClient.class, SNSConfig.class, SNSClientImpl.class})
@Testcontainers
public class SendSnsTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    private SNSClient snsClient;

    @Autowired
    private Ab2dEnvironment environment;
    @Test
    void testSendSns() {
        assertDoesNotThrow(() -> {
            snsClient.sendMessage(Topics.COVERAGE_COUNTS.getValue(), "test");
        });
    }

    @Test
    void testNoUrl() {
        System.clearProperty("cloud.aws.end-point.uri");
        SNSConfig config = new SNSConfig();
        SNSClientImpl client = new SNSClientImpl(config.amazonSNS(), environment);
        assertDoesNotThrow(() -> {
            client.sendMessage(Topics.COVERAGE_COUNTS.getValue(), "test");
        });
    }


}
