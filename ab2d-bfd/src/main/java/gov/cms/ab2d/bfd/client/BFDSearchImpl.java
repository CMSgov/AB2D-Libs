package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class BFDSearchImpl implements BFDSearch {

    private final HttpClient httpClient;
    private final Environment environment;
    private final BfdClientVersions bfdClientVersions;

    public BFDSearchImpl(HttpClient httpClient, Environment environment, BfdClientVersions bfdClientVersions) {
        this.httpClient = httpClient;
        this.environment = environment;
        this.bfdClientVersions = bfdClientVersions;
    }

    /**
     * Search for ebos using the provided parameters
     * AB2D does not report SAMHSA claims (https://www.samhsa.gov/).
     *      - excludeSAMHSA must be set to true to maintain this exclusion
     *
     * @param patientId internal beneficiary id
     * @param since the minimum lastUpdated date which may be null
     * @param pageSize maximum number of records that can be returned
     * @param bulkJobId header to uniquely identify what job this is coming from within BFD logs
     * @param version the version of FHIR that we need from BFD
     * @return a bundle of eobs for the patient.
     * @throws IOException on failure to retrieve claims from BFD
     */
    @Trace
    @Override
    public IBaseBundle searchEOB(long patientId, OffsetDateTime since, OffsetDateTime until, int pageSize, String bulkJobId, FhirVersion version, String contractNum) throws IOException {
        StringBuilder url = new StringBuilder(bfdClientVersions.getUrl(version) + "ExplanationOfBenefit/_search");

        if (pageSize > 0) {
            url.append("&_count=").append(pageSize);
        }

        HttpPost request = new HttpPost(url.toString());

        // No active profiles means use JSON
        if (environment.getActiveProfiles().length == 0) {
            request.addHeader("Accept", "application/fhir+json;q=1.0, application/json+fhir;q=0.9");
        }

        request.addHeader(HttpHeaders.ACCEPT, "gzip");
        request.addHeader(HttpHeaders.ACCEPT_CHARSET, "utf-8");
        request.addHeader(BFDClient.BFD_HDR_BULK_CLIENTID, contractNum);
        request.addHeader(BFDClient.BFD_HDR_BULK_JOBID, bulkJobId);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("patient", "" + patientId));
        params.add(new BasicNameValuePair("excludeSAMHSA", "true"));
        if (since != null) {
            params.add(new BasicNameValuePair("_lastUpdated", "ge" + since));
        }

        if (until != null) {
            params.add(new BasicNameValuePair("_lastUpdated", "le" + until));
        }

        request.setEntity(new UrlEncodedFormEntity(params));
        log.info("Executing BFD Search Request {}", request);
        byte[] responseBytes = getEOBSFromBFD(patientId, request);

        return parseBundle(version, responseBytes);
    }

    /**
     * Method exists to track connection to BFD for New Relic
     */
    @Trace
    private byte[] getEOBSFromBFD(long patientId, HttpPost request) throws IOException {
        byte[] responseBytes;
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {

                try (InputStream instream = response.getEntity().getContent()) {
                    responseBytes = instream.readAllBytes();
                }

            } else if (status == HttpStatus.SC_NOT_FOUND) {
                throw new ResourceNotFoundException("Patient " + patientId + " was not found");
            } else {
                throw new RuntimeException("Server error occurred");
            }
        }
        return responseBytes;
    }

    @Trace
    private IBaseBundle parseBundle(FhirVersion version, byte[] responseBytes) {
        return version.getJsonParser().parseResource(version.getBundleClass(), new ByteArrayInputStream(responseBytes));
    }
}