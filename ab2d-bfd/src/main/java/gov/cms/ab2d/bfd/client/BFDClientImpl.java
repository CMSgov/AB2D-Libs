package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleUtil;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Credits: most of the code in this class has been copied over from https://github
 * .com/CMSgov/dpc-app
 */
@Component
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientImpl implements BFDClient {

    public static final String PTDCNTRCT_URL_PREFIX = "https://bluebutton.cms.gov/resources/variables/ptdcntrct";
    public static final String YEAR_URL_PREFIX = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";

    @Value("${bfd.eob.pagesize}")
    private int pageSize;

    @Value("${bfd.contract.to.bene.pagesize}")
    private int contractToBenePageSize;

    private final BFDSearch bfdSearch;
    private final BfdClientVersions bfdClientVersions;

    private static final String INCLUDE_IDENTIFIERS_HEADER = "IncludeIdentifiers";
    private static final String MBI_HEADER_VALUE = "mbi";

    public BFDClientImpl(BFDSearch bfdSearch, BfdClientVersions bfdClientVersions) {
        this.bfdSearch = bfdSearch;
        this.bfdClientVersions = bfdClientVersions;
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * <p>
     * There are two edge cases to consider when pulling EoB data given a patientID:
     * 1. No patient with the given ID exists: if this is the case, BlueButton should return a
     * Bundle with no
     * entry, i.e. ret.hasEntry() will evaluate to false. For this case, the method will throw a
     * {@link ResourceNotFoundException}
     * <p>
     * 2. A patient with the given ID exists, but has no associated EoB records: if this is the
     * case, BlueButton should
     * return a Bundle with an entry of size 0, i.e. ret.getEntry().size() == 0. For this case,
     * the method simply
     * returns the Bundle it received from BlueButton to the caller, and the caller is
     * responsible for handling Bundles
     * that contain no EoBs.
     *
     * @param patientID The requested patient's ID
     * @return {@link org.hl7.fhir.instance.model.api.IBaseBundle} Containing a number (possibly 0) of Resources
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Trace
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class}
    )
    public IBaseBundle requestEOBFromServer(FhirVersion version, long patientID, String contractNum) {
        return requestEOBFromServer(version, patientID, null, null, contractNum);
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * similar to {@link #requestEOBFromServer(FhirVersion, long, String)} but includes a date filter in which the
     * _lastUpdated date must be after
     * <p>
     *
     * @param version   The FHIR version
     * @param patientID The requested patient's ID
     * @param sinceTime The start date for the request
     * @param untilTime The stop date for the request
     * @return {@link IBaseBundle} Containing a number (possibly 0) of Resources
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Trace
    @SneakyThrows
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class}
    )
    public IBaseBundle requestEOBFromServer(FhirVersion version, long patientID, OffsetDateTime sinceTime, OffsetDateTime untilTime, String contractNum) {
        final Segment bfdSegment = NewRelic.getAgent().getTransaction().startSegment("BFD Call for patient with patient ID " + patientID +
                " using since " + sinceTime + " and until " + untilTime);
        bfdSegment.setMetricName("RequestEOB");

        IBaseBundle result = bfdSearch.searchEOB(patientID, sinceTime, untilTime, pageSize, getJobId(), version, contractNum);

        bfdSegment.end();

        return result;
    }

    @Trace
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class}
    )
    public IBaseBundle requestNextBundleFromServer(FhirVersion version, IBaseBundle bundle, String contractNum) {

        String nextPageUrl = BundleUtil.getLinkUrlOfType(bfdClientVersions.getClient(version).getFhirContext(), bundle, "next");

        log.error("-------- nextPageUrl " + nextPageUrl);

        try {
            if (nextPageUrl.contains("Coverage.extension"))
                return requestPartDEnrolleesWithCursor(version, nextPageUrl);
            if (nextPageUrl.contains("ExplanationOfBenefit"))
                return searchEOB(version, contractNum, nextPageUrl);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private IBaseBundle searchEOB(FhirVersion version, String contractNum, String url) throws IOException {
        String query = url.substring(url.indexOf('?') + 1);

        Map<String, List<String>> params = Arrays.stream(query.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.groupingBy(
                        pair -> pair[0],
                        Collectors.mapping(pair -> pair[1], Collectors.toList())
                ));

        String patient = params.getOrDefault("patient", List.of()).stream().findFirst().orElse(null);
        String countStr = params.getOrDefault("count", List.of()).stream().findFirst().orElse(null);
        int count = (countStr == null) ? pageSize : Integer.parseInt(countStr);
        String startIndex = params.getOrDefault("startIndex", List.of()).stream().findFirst().orElse(null);

        List<String> lastList = params.getOrDefault("_lastUpdated", List.of());
        String since = lastList.stream()
                .filter(v -> v.startsWith("ge"))
                .map(v -> v.substring(2))
                .findFirst().orElse(null);
        String until = lastList.stream()
                .filter(v -> v.startsWith("le"))
                .map(v -> v.substring(2))
                .findFirst().orElse(null);
        log.info("bfdSearch.searchEOBString " + patient + " " + count + " " + startIndex + " " + since + " " + until);
        return bfdSearch.searchEOBString(patient, since, until, count, getJobId(), version, contractNum, startIndex);
    }

    private IBaseBundle requestPartDEnrolleesWithCursor(FhirVersion version, String url) throws UnsupportedEncodingException {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String rawQuery = uri.getRawQuery();

        // Decode into a map of parameter → value
        Map<String, String> params = new HashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            params.put(key, value);
        }

        String extensionParam = params.get("_has:Coverage.extension");
        String referenceYearParam = params.get("_has:Coverage.rfrncyr");

        String[] extParts = extensionParam.split("\\|", 2);
        String contract = extParts[1];

        String year = referenceYearParam.split("\\|", 2)[1];

        String month = extParts[0].substring(extParts[0].length() - 2);
        log.info("requestPartDEnrolleesWithCursor " + contract + " " + year + " " + month + " " + params.get("cursor"));
        return requestPartDEnrolleesFromServer(version, contract, month, year, params.get("cursor"));
    }

    private String getJobId() {
        var jobId = BFDClient.BFD_BULK_JOB_ID.get();
        if (jobId == null) {
            log.warn("BFD Bulk Job Id not set: " + new Throwable());  // Capture the stack trace for diagnosis
            jobId = "UNKNOWN";
        }
        return jobId;
    }

    @Trace
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class, InvalidRequestException.class}
    )
    public IBaseBundle requestPartDEnrolleesFromServer(FhirVersion version, String contractNumber, int month) {
        var monthParameter = createMonthParameter(month);
        var monthCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);
        var request = bfdClientVersions.getClient(version).search()
                .forResource(version.getPatientClass())
                .where(monthCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, contractNumber)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader(INCLUDE_IDENTIFIERS_HEADER, MBI_HEADER_VALUE)
                .count(contractToBenePageSize)
                .usingStyle(SearchStyleEnum.POST);
        log.info("Executing request to get Part D Enrollees " + request);
        return request.returnBundle(version.getBundleClass())
                .encodedJson()
                .execute();
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class, InvalidRequestException.class}
    )
    public IBaseBundle requestPartDEnrolleesFromServer(FhirVersion version, String contractNumber, int month, int year) {
        var monthParameter = createMonthParameter(month);
        var monthCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);
        var yearCriterion = new TokenClientParam("_has:Coverage.rfrncyr")
                .exactly()
                .systemAndIdentifier(YEAR_URL_PREFIX, createYearParameter(year));
        var request = bfdClientVersions.getClient(version).search()
                .forResource(version.getPatientClass())
                .where(monthCriterion)
                .and(yearCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, contractNumber)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader(INCLUDE_IDENTIFIERS_HEADER, MBI_HEADER_VALUE)
                .count(contractToBenePageSize)
                .usingStyle(SearchStyleEnum.POST);
        log.info("Executing request to get Part D Enrollees " + request);
        return request.returnBundle(version.getBundleClass())
                .encodedJson()
                .execute();
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class, InvalidRequestException.class}
    )
    public IBaseBundle requestPartDEnrolleesFromServer(FhirVersion version, String contractNumber, String month, String year, String cursor) {
        var monthCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(month, contractNumber);
        var yearCriterion = new TokenClientParam("_has:Coverage.rfrncyr")
                .exactly()
                .systemAndIdentifier(YEAR_URL_PREFIX, year);
        var request = bfdClientVersions.getClient(version).search()
                .forResource(version.getPatientClass())
                .where(monthCriterion)
                .and(yearCriterion)
                .and(new StringClientParam("cursor").matches().value(cursor))
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, contractNumber)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader(INCLUDE_IDENTIFIERS_HEADER, MBI_HEADER_VALUE)
                .count(contractToBenePageSize)
                .usingStyle(SearchStyleEnum.POST);
        log.info("Executing request to get Part D Enrollees " + request);
        return request.returnBundle(version.getBundleClass())
                .encodedJson()
                .execute();
    }

    // Pad year to expected four digits
    private String createYearParameter(int year) {
        return StringUtils.leftPad("" + year, 4, '0');
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = {ResourceNotFoundException.class}
    )
    public IBaseConformance capabilityStatement(FhirVersion version) {
        try {
            return getCapabilityStatement(version);
        } catch (Exception ex) {
            return null;
        }
    }

    private IBaseConformance getCapabilityStatement(FhirVersion version) {
        Class<? extends IBaseConformance> resource = version.getCapabilityClass();
        return bfdClientVersions.getClient(version).capabilities()
                .ofType(resource)
                .execute();
    }

    private String createMonthParameter(int month) {
        final String zeroPaddedMonth = StringUtils.leftPad("" + month, 2, '0');
        return PTDCNTRCT_URL_PREFIX + zeroPaddedMonth;
    }
}
