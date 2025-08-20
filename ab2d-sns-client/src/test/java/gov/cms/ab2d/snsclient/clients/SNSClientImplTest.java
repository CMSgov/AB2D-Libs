package gov.cms.ab2d.snsclient.clients;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.snsclient.clients.SNSClientImpl.getSnsTopicPrefix;
import static org.junit.jupiter.api.Assertions.*;

class SNSClientImplTest {

    @Test
    void testTopicPrefix() {
        assertEquals("ab2d-dev", getSnsTopicPrefix(Ab2dEnvironment.DEV));
        assertEquals("ab2d-test", getSnsTopicPrefix(Ab2dEnvironment.IMPL));
        assertEquals("ab2d-sandbox", getSnsTopicPrefix(Ab2dEnvironment.SANDBOX));
        assertEquals("ab2d-prod", getSnsTopicPrefix(Ab2dEnvironment.PRODUCTION));

    }
}
