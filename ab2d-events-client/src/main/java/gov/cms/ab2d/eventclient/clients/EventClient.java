package gov.cms.ab2d.eventclient.clients;

import gov.cms.ab2d.eventclient.events.LoggableEvent;

public interface EventClient {
    void sendLogs(LoggableEvent requestEvent);
}
