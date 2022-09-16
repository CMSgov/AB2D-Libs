package gov.cms.ab2d.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Data
@Builder
public class MetricAlarm {
    private String AlarmName;
    private String AlarmDescription;
    private String AWSAccountId;
    private String AlarmConfigurationUpdatedTimestamp;
    private String NewStateValue;
    private String NewStateReason;
    private String StateChangeTime;
    private String Region;
    private String AlarmArn;
    private String OldStateValue;
    private List<String> OKActions;
    private List<String> AlarmActions;
    private List<Object> InsufficientDataActions;
    private Trigger Trigger;

    public String getNamespace() {
        return Optional.ofNullable(Trigger)
                .orElse(new Trigger())
                .getNamespace();
    }
}
