package gov.cms.ab2d.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Trigger {
    private Dimensions[] Dimensions;
    private String MetricName;
    private String Namespace;
    private String StatisticType;
    private String Statistic;
    private String Unit;
    private int Period;
    private String EvaluationPeriods;
    private String ComparisonOperator;
    private int Threshold;
    private String TreatMissingData;
    private String EvaluateLowSampleCountPercentile;
}
