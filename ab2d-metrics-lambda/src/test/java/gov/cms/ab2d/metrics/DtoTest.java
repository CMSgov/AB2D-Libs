package gov.cms.ab2d.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoTest {
    @Test
    void DimensionsTest() {
        Dimensions dimensions = Dimensions.builder()
                .name("")
                .value("")
                .build();
        dimensions.setName("a");
        dimensions.setValue("a");
        assertEquals("a", dimensions.getName());
        assertEquals("a", dimensions.getValue());
    }

    @Test
    void MetricAlarmTest() {
        MetricAlarm metricAlarm = MetricAlarm.builder()
                .AlarmName("")
                .AlarmDescription("")
                .AWSAccountId("")
                .AlarmConfigurationUpdatedTimestamp("")
                .NewStateValue("")
                .NewStateReason("")
                .StateChangeTime("")
                .Region("")
                .AlarmArn("")
                .OldStateValue("")
                .AlarmActions(new ArrayList<>())
                .OKActions(new ArrayList<>())
                .InsufficientDataActions(new ArrayList<>())
                .Trigger(Trigger.builder()
                        .build())
                .build();

        metricAlarm.setAlarmName("");
        metricAlarm.setAlarmDescription("");
        metricAlarm.setAWSAccountId("");
        metricAlarm.setAlarmConfigurationUpdatedTimestamp("");
        metricAlarm.setNewStateValue("");
        metricAlarm.setNewStateReason("");
        metricAlarm.setStateChangeTime("");
        metricAlarm.setRegion("");
        metricAlarm.setAlarmArn("");
        metricAlarm.setOldStateValue("");
        metricAlarm.setAlarmActions(new ArrayList<>());
        metricAlarm.setOKActions(new ArrayList<>());
        metricAlarm.setInsufficientDataActions(new ArrayList<>());
        metricAlarm.setTrigger(new Trigger());

        assertEquals("", metricAlarm.getAlarmName());
        assertEquals("", metricAlarm.getAlarmDescription());
        assertEquals("", metricAlarm.getAWSAccountId());
        assertEquals("", metricAlarm.getAlarmConfigurationUpdatedTimestamp());
        assertEquals("", metricAlarm.getNewStateValue());
        assertEquals("", metricAlarm.getNewStateReason());
        assertEquals("", metricAlarm.getStateChangeTime());
        assertEquals("", metricAlarm.getRegion());
        assertEquals("", metricAlarm.getAlarmArn());
        assertEquals("", metricAlarm.getOldStateValue());
        assertEquals(new ArrayList<>(), metricAlarm.getAlarmActions());
        assertEquals(new ArrayList<>(), metricAlarm.getOKActions());
        assertEquals(new ArrayList<>(), metricAlarm.getInsufficientDataActions());
        assertEquals(new Trigger(), metricAlarm.getTrigger());
    }

    @Test
    void TriggerTest() {
        Dimensions[] dimensions = new Dimensions[]{};
        Trigger trigger = Trigger.builder()
                .Dimensions(dimensions)
                .MetricName("")
                .Statistic("")
                .StatisticType("")
                .Unit("")
                .Period(1)
                .EvaluationPeriods("")
                .ComparisonOperator("")
                .Threshold(1)
                .TreatMissingData("")
                .EvaluateLowSampleCountPercentile("")
                .Namespace("")
                .ComparisonOperator("")
                .build();

        trigger.setDimensions(dimensions);
        trigger.setMetricName("test");
        trigger.setStatistic("test");
        trigger.setStatisticType("test");
        trigger.setUnit("test");
        trigger.setPeriod(1);
        trigger.setEvaluationPeriods("test");
        trigger.setComparisonOperator("test");
        trigger.setThreshold(1);
        trigger.setTreatMissingData("test");
        trigger.setEvaluateLowSampleCountPercentile("test");
        trigger.setNamespace("test");
        trigger.setComparisonOperator("test");

        assertEquals(dimensions, trigger.getDimensions());
        assertEquals("test", trigger.getMetricName());
        assertEquals("test", trigger.getStatistic());
        assertEquals("test", trigger.getStatisticType());
        assertEquals("test", trigger.getUnit());
        assertEquals(1, trigger.getPeriod());
        assertEquals("test", trigger.getEvaluationPeriods());
        assertEquals("test", trigger.getComparisonOperator());
        assertEquals(1, trigger.getThreshold());
        assertEquals("test", trigger.getTreatMissingData());
        assertEquals("test", trigger.getEvaluateLowSampleCountPercentile());
        assertEquals("test", trigger.getNamespace());
        assertEquals("test", trigger.getComparisonOperator());
    }

}
