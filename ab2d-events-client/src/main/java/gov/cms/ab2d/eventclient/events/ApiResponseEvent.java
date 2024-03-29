package gov.cms.ab2d.eventclient.events;


import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

/**
 * Class to create and log an API request sent back to the user
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApiResponseEvent extends LoggableEvent {
    // The HTTP response code
    private int responseCode;
    // The response sent - assuming not all the data
    private String responseString;
    // A description giving context to the response
    private String description;
    // The unique id that this response is a response to
    private String requestId;

    public ApiResponseEvent() { }

    public ApiResponseEvent(String organization, String jobId, HttpStatus responseCode, String responseString, String description,
                            String requestId) {
        super(OffsetDateTime.now(), organization, jobId);
        if (responseCode != null) {
            this.responseCode = responseCode.value();
        }
        this.responseString = responseString;
        this.description = description;
        this.requestId = requestId;
    }

    @Override
    public String asMessage() {
        return String.format("(%s): %s", responseCode, description);
    }
}
