package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.bfd.client.MockUtils.getRawJson;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

/**
 * Credits: most of the code in this class has been adopted from https://github.com/CMSgov/dpc-app
 */
@SpringBootTest(classes = SpringBootApp.class)
@ContextConfiguration(initializers = {BlueButtonClientSTU3Test.PropertyOverrider.class})
public class BlueButtonClientSTU3Test {
    // A random example patient (Jane Doe)
    private static final Long TEST_PATIENT_ID = 20140000008325L;
    // A patient that only has a single EOB record in bluebutton
    private static final Long TEST_SINGLE_EOB_PATIENT_ID = 20140000009893L;
    // A patient id that should not exist in bluebutton
    private static final Long TEST_NONEXISTENT_PATIENT_ID = 31337L;
    private static final Long TEST_SLOW_PATIENT_ID = 20010000001111L;
    private static final Long TEST_NO_RECORD_PATIENT_ID = 20010000001115L;
    private static final Long TEST_NO_RECORD_PATIENT_ID_MBI = 20010000001116L;

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.json";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    private static final String[] CONTRACT_MONTHS = {"ptdcntrct01", "ptdcntrct02", "ptdcntrct03", "ptdcntrct04",
            "ptdcntrct05", "ptdcntrct06", "ptdcntrct07", "ptdcntrct08", "ptdcntrct09", "ptdcntrct10",
            "ptdcntrct11", "ptdcntrct12"
    };
    private static final String CONTRACT = "S00001";
    public static final int MOCK_PORT_V1 = MockUtils.randomMockServerPort();

    // Leave so code coverage works
    @Autowired
    private BFDClientImpl bbc;

    private static ClientAndServer mockServer;

    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.url=http://localhost:" + MOCK_PORT_V1;
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(MOCK_PORT_V1);
        MockUtils.createMockServerExpectation("/v1/fhir/metadata", HttpStatus.SC_OK,
                getRawJson(METADATA_PATH), List
                        .of(), MOCK_PORT_V1);

        // Ensure timeouts are working.
        MockUtils.createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                StringUtils.EMPTY,
                Collections.singletonList(Parameter.param("patient", TEST_SLOW_PATIENT_ID.toString())),
                8000,
                MOCK_PORT_V1
        );

        for (String patientId : TEST_PATIENT_IDS) {
            MockUtils.createMockServerExpectation(
                    "/v1/fhir/Patient/" + patientId,
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".json"),
                    List.of(),
                    MOCK_PORT_V1
            );

            MockUtils.createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_EOB_PATH_PREFIX + patientId + ".json"),
                    List.of(Parameter.param("patient", patientId),
                            Parameter.param("excludeSAMHSA", "true")),
                    MOCK_PORT_V1
            );
        }

        MockUtils.createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.json"),
                List.of(),
                MOCK_PORT_V1
        );

        // Patient that exists, but has no records
        MockUtils.createMockServerExpectation(
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID,
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".json"),
                List.of(),
                MOCK_PORT_V1
        );
        MockUtils.createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".json"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID.toString()),
                        Parameter.param("excludeSAMHSA", "true")),
                MOCK_PORT_V1
        );

        MockUtils.createMockServerExpectation(
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID_MBI,
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID_MBI + ".json"),
                List.of(),
                MOCK_PORT_V1
        );
        MockUtils.createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID_MBI + ".json"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID_MBI.toString()),
                        Parameter.param("excludeSAMHSA", "true")),
                MOCK_PORT_V1
        );

        // Create mocks for pages of the results
        for (String startIndex : List.of("10", "20", "30")) {
            MockUtils.createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + startIndex + ".json"),
                    List.of(Parameter.param("patient", TEST_PATIENT_ID.toString()),
                            Parameter.param("count", "10"),
                            Parameter.param("startIndex", startIndex),
                            Parameter.param("excludeSAMHSA", "true")),
                    MOCK_PORT_V1
            );
        }

        for (String month : CONTRACT_MONTHS) {
            MockUtils.createMockServerExpectation(
                    "/v1/fhir/Patient/",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.json"),
                    List.of(Parameter.param("_has:Coverage.extension",
                            "https://bluebutton.cms.gov/resources/variables/ptdcntrct" + month + "|" + CONTRACT)),
                    MOCK_PORT_V1
            );
        }
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    void shouldGetTimedOutOnSlowResponse() {
        var exception = Assertions.assertThrows(SocketTimeoutException.class, () -> bbc.requestEOBFromServer(STU3, TEST_SLOW_PATIENT_ID, CONTRACT));

        var rootCause = ExceptionUtils.getRootCause(exception);
        assertTrue(rootCause instanceof SocketTimeoutException);
        assertEquals("Read timed out", rootCause.getMessage());

    }

    @Test
    void shouldGetEOBFromPatientID() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, CONTRACT);

        validation(response);
    }

    @Test
    void shouldGetEOBFromPatientIDSince() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, null, null, CONTRACT);

        validation(response);
    }

    @Test
    void shouldGetEOBFromPatientIDUtil() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, null, null, CONTRACT);

        validation(response);
    }

    @Test
    void shouldGetEOBFromPatientIDSinceAndUtil() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, OffsetDateTime.parse(
                "2020-02-13T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME), null, CONTRACT);

        validation(response);
    }

    private static void validation(Bundle response) {
        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(10, response.getEntry().size(), "The demo patient should have exactly 10 EOBs");
    }

    @Test
    void shouldGetEOBPatientNoRecords() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_NO_RECORD_PATIENT_ID, CONTRACT);
        assertFalse(response.hasEntry());
    }

    @Test
    void shouldGetEOBPatientNoRecordsMBI() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_NO_RECORD_PATIENT_ID_MBI, CONTRACT);
        assertFalse(response.hasEntry());
    }

    @Test
    void shouldNotHaveNextBundle() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_SINGLE_EOB_PATIENT_ID, CONTRACT);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(1, response.getEntry().size(), "The demo patient should have exactly 1 EOBs");
        assertNull(response.getLink(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");
    }

    @Test
    void shouldHaveNextBundle() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, CONTRACT);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");

        // Change url to point to random mock server port instead of default port
        response.getLink().forEach(link -> {
            String url = link.getUrl().replace("localhost:8083", "localhost:" + MOCK_PORT_V1);
            link.setUrl(url);
        });

        org.hl7.fhir.dstu3.model.Bundle nextResponse = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestNextBundleFromServer(STU3, response, CONTRACT);
        assertNotNull(nextResponse, "Should have a next bundle");
        assertEquals(10, nextResponse.getEntry().size());
    }

    @Test
    void shouldReturnBundleContainingOnlyEOBs() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, CONTRACT);

        response.getEntry().forEach(entry -> assertEquals(
                org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit,
                entry.getResource().getResourceType(),
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    void shouldHandlePatientsWithOnlyOneEOB() {
        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_SINGLE_EOB_PATIENT_ID, CONTRACT);
        assertEquals(1, response.getEntry().size(), "This demo patient should have exactly 1 EOB");
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestEOBFromServer(STU3, TEST_NONEXISTENT_PATIENT_ID, CONTRACT),
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a " +
                        "non-existent patient"
        );
    }

    @Test
    void shouldGetPatientBundleFromPartDEnrolleeRequest() {
        for (int i = 1; i <= 12; i++) {
            org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestPartDEnrolleesFromServer(STU3, CONTRACT, i);

            assertNotNull(response, "There should be a non null patient bundle");
            assertEquals(3, response.getEntry().size(), "The bundle has 2 patients");
        }
    }

    @Test
    void shouldGetMetadata() {
        org.hl7.fhir.dstu3.model.CapabilityStatement capabilityStatement = (org.hl7.fhir.dstu3.model.CapabilityStatement) bbc.capabilityStatement(STU3);

        assertNotNull(capabilityStatement, "There should be a non null capability statement");
        assertEquals("3.0.1", capabilityStatement.getFhirVersion());
        assertEquals(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE, capabilityStatement.getStatus());
    }
}