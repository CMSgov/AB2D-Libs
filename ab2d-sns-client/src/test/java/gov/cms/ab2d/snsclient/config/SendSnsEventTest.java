package gov.cms.ab2d.snsclient.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
@Testcontainers
public class SendSnsEventTest {

    public static final String LOCAL_EVENTS_SQS = "local-events-sqs";
    public static final String DEV_EVENTS_SQS = "dev-events-sqs";
    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();
    @Autowired
    private AmazonSQS amazonSQS;


    @Test
    void testQueueUrl() {
        String url = amazonSQS.getQueueUrl(LOCAL_EVENTS_SQS)
                .getQueueUrl();
        assertTrue(url.contains("localhost:"));
        assertTrue(url.contains(LOCAL_EVENTS_SQS));
    }

    @Test
    void testQueueUrlNoExistingQueue() {
        Assertions.assertThrows(QueueDoesNotExistException.class, () -> {
            amazonSQS.getQueueUrl(DEV_EVENTS_SQS);
        });
    }

}
