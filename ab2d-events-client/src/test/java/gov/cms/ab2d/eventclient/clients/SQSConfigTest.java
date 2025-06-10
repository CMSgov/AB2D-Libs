package gov.cms.ab2d.eventclient.clients;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SQSConfigTest {

    String url = "https://sqs.us-east-1.amazonaws.com/123456789/ab2d-dev-events-sqs";

    @Test
    void deriveSqsQueueName() {
        assertEquals("ab2d-dev-events-sqs", SQSConfig.deriveSqsQueueName(url));
    }

}
