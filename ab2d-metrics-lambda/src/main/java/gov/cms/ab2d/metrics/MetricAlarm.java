package gov.cms.ab2d.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MetricAlarm {
    @JsonProperty("AlarmName")
    private String alarmName;
    @JsonProperty("AlarmDescription")
    private String alarmDescription;
    @JsonProperty("AWSAccountId")
    private String awsAccountId;
    @JsonProperty("AlarmConfigurationUpdatedTimestamp")
    private String alarmConfigurationUpdatedTimestamp;
    @JsonProperty("NewStateValue")
    private String newStateValue;
    @JsonProperty("NewStateReason")
    private String newStateReason;
    @JsonProperty("StateChangeTime")
    private String stateChangeTime;
    @JsonProperty("Region")
    private String region;
    @JsonProperty("AlarmArn")
    private String alarmArn;
    @JsonProperty("OldStateValue")
    private String oldStateValue;
    @JsonProperty("OKActions")
    private List<String> okActions;
    @JsonProperty("AlarmActions")
    private List<String> alarmActions;
    @JsonProperty("InsufficientDataActions")
    private List<Object> insufficientDataActions;
    @JsonProperty("Trigger")
    private Trigger trigger;

    @JsonProperty("Namespace")
    public String getNamespace() {
        return Optional.ofNullable(trigger)
                .orElse(new Trigger())
                .getNamespace();
    }
}
