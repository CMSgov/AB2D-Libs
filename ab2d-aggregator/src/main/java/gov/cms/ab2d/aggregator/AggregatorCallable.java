package gov.cms.ab2d.aggregator;

import lombok.Getter;

import java.util.concurrent.Callable;

/**
 * This Callable allows us to hover over the file directory until all data is streamed out
 * by the worker and occasionally aggregate
 */
@Getter
public class AggregatorCallable implements Callable<Integer> {
    private final String jobId;
    private final String contractId;

    public AggregatorCallable(String jobId, String contractId) {
        this.jobId = jobId;
        this.contractId = contractId;
    }

    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public Integer call() throws Exception {
        int numAggregations = 0;
        // Create a new aggregator for the job
        Aggregator aggregator = new Aggregator(jobId, contractId);
        // While the worker isn't done with streaming files
        while (!StatusManager.isJobDoneStreamingData(jobId)) {
            // aggregate data files
            while (aggregator.aggregate(false)) {
                numAggregations++;
            }
            // aggregate error files
            while (aggregator.aggregate(true)) {
                numAggregations++;
            }
            // Sleep a little between checks
            Thread.sleep(1000);
        }
        // We've taken all the files that the worker has given us, "finish" the job so that
        // the worker knows we're done
        JobHelper.aggregatorFinishJob(jobId);
        return numAggregations;
    }
}
