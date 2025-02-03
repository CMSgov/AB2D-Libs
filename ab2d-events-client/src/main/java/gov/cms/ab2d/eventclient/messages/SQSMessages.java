package gov.cms.ab2d.eventclient.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class SQSMessages {
    protected SQSMessages() { }
}

