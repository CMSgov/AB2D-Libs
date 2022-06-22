package gov.cms.ab2d.eventclient.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventclient.events.LoggableEvent;

public interface EventClient {
    void send(LoggableEvent requestEvent);
}
