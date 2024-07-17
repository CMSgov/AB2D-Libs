package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.fhir.PatientIdentifier.CURRENT_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.Currency.CURRENT;
import static gov.cms.ab2d.fhir.PatientIdentifier.Currency.HISTORIC;
import static gov.cms.ab2d.fhir.PatientIdentifier.HISTORIC_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.MBI_ID;
import static gov.cms.ab2d.fhir.IdentifierUtils.CURRENCY_IDENTIFIER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PatientIdentifierUtilsTest {

    @Test
    void testGetTypes() {
        assertEquals(PatientIdentifier.MBI_ID, PatientIdentifier.Type.MBI.getSystem());
        assertEquals(PatientIdentifier.Type.MBI, PatientIdentifier.Type.fromSystem(PatientIdentifier.MBI_ID));
        assertEquals(null, PatientIdentifier.Type.fromSystem("does-not-exist"));
    }

    @Test
    void testGetValueAsLong() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        assertNull(patientIdentifier.getValueAsLong());

        patientIdentifier.setValue("1234");
        assertEquals(1234, patientIdentifier.getValueAsLong());
    }

    @Test
    void testGetMbis() {
        Patient patient = new Patient();
        assertFalse(BundleUtils.isExplanationOfBenefitResource(patient));
        assertNull(IdentifierUtils.getCurrentMbi(null));
        assertNull(IdentifierUtils.getHistoricMbi(null));
        assertNull(IdentifierUtils.getBeneId(null));
        Identifier identifier = new Identifier();
        identifier.setSystem("https://bluebutton.cms.gov/resources/variables/bene_id");
        identifier.setValue("test-1");
        Identifier identifier2 = new Identifier();
        identifier2.setSystem(MBI_ID);
        identifier2.setValue("mbi-1");
        Extension extension = new Extension().setUrl(CURRENCY_IDENTIFIER).setValue(new Coding().setCode(CURRENT_MBI));
        identifier2.addExtension(extension);

        var identifier3 = new Identifier();
        identifier3.setSystem(MBI_ID);
        identifier3.setValue("mbi-2");
        Extension extension2 = new Extension().setUrl(CURRENCY_IDENTIFIER).setValue(new Coding().setCode(HISTORIC_MBI));
        identifier3.addExtension(extension2);
        patient.setIdentifier(List.of(identifier, identifier2, identifier3));

        List<PatientIdentifier> identifiers = IdentifierUtils.getIdentifiers(patient);
        assertEquals("mbi-1", IdentifierUtils.getCurrentMbi(identifiers).getValue());
        Set<PatientIdentifier> historical = IdentifierUtils.getHistoricMbi(identifiers);
        PatientIdentifier h = (PatientIdentifier) historical.toArray()[0];
        assertEquals("mbi-2", h.getValue());
        assertEquals("test-1", IdentifierUtils.getBeneId(identifiers).getValue());
    }

    @Test
    void testStu3ExtractIds() throws IOException {
        Bundle resource = (Bundle) extractBundle(FhirVersion.STU3, "data/stu3patients.json");
        for (Bundle.BundleEntryComponent component : resource.getEntry()) {
            List<PatientIdentifier> ids = IdentifierUtils.getIdentifiers((Patient) component.getResource());
            assertEquals(5, ids.size());
            PatientIdentifier benId = IdentifierUtils.getBeneId(ids);
            assertEquals("-19990000001101", benId.getValue());
            assertEquals(PatientIdentifier.Type.BENE_ID, benId.getType());
            assertEquals(CURRENT, benId.getCurrency());
            PatientIdentifier mbiCurrentId = IdentifierUtils.getCurrentMbi(ids);
            assertEquals("3S24A00AA00", mbiCurrentId.getValue());
            Set<PatientIdentifier> historicalMbi = IdentifierUtils.getHistoricMbi(ids);
            assertEquals(1, historicalMbi.size());
            PatientIdentifier hist = historicalMbi.stream().findFirst().get();
            assertEquals("11111111111", hist.getValue());
            assertEquals(HISTORIC, hist.getCurrency());
        }
    }

    @Test
    void testNullInputs() {
        assertNull(IdentifierUtils.getIdentifiers(null));
    }

    @Test
    void testEmptyPatients() {
        Patient patient = new Patient();
        assertTrue(IdentifierUtils.getIdentifiers(patient).isEmpty());
    }

    @Test
    void testEmptyPatientIds() {
        Patient patient = new Patient();
        patient.setIdentifier(List.of());
        assertTrue(IdentifierUtils.getIdentifiers(patient).isEmpty());
    }

    @Test
    void testMockInput() {
        assertNull(IdentifierUtils.getIdentifier(mock(ICompositeType.class)));
    }

    @Test
    void testRealInput() {
        Patient patient = new Patient();
        Identifier identifier = new Identifier();
        identifier.setSystem("https://bluebutton.cms.gov/resources/variables/bene_id");
        identifier.setValue("test-1");
        patient.setIdentifier(List.of(identifier));
        assertNotNull(IdentifierUtils.getIdentifier(patient.getIdentifier().get(0)));
    }

    @Test
    void testGetBeneId() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.BENE_ID);
        patientIdentifier.setValue("test-1");
        assertEquals("test-1", IdentifierUtils.getBeneId(List.of(patientIdentifier)).getValue());
    }

    @Test
    void testGetBeneIdWrongType() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.MBI);
        patientIdentifier.setValue("test-1");
        assertNull(IdentifierUtils.getBeneId(List.of(patientIdentifier)));
    }

    @Test
    void testGetCurrentMbiCurrent() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.MBI);
        patientIdentifier.setValue("test-1");
        patientIdentifier.setCurrency(PatientIdentifier.Currency.CURRENT);
        assertEquals("test-1", IdentifierUtils.getCurrentMbi(List.of(patientIdentifier)).getValue());
    }

    @Test
    void testGetCurrentMbiHistoric() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.MBI);
        patientIdentifier.setValue("test-1");
        patientIdentifier.setCurrency(PatientIdentifier.Currency.HISTORIC);
        assertNull(IdentifierUtils.getCurrentMbi(List.of(patientIdentifier)));
    }

    @Test
    void testGetCurrentMbiUnknown() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.MBI);
        patientIdentifier.setValue("test-1");
        patientIdentifier.setCurrency(PatientIdentifier.Currency.UNKNOWN);
        assertEquals("test-1", IdentifierUtils.getCurrentMbi(List.of(patientIdentifier)).getValue());
    }

    @Test
    void testGetCurrentMbiWrongType() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.BENE_ID);
        patientIdentifier.setValue("test-1");
        assertNull(IdentifierUtils.getCurrentMbi(List.of(patientIdentifier)));
    }

    @Test
    void testGetCurrentMbiTypeNotExists() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(null);
        patientIdentifier.setValue("test-1");
        assertNull(IdentifierUtils.getCurrentMbi(List.of(patientIdentifier)));
    }

    @Test
    void testReturnsFalseIfCodingNotExist() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setType(PatientIdentifier.Type.MBI);
        patientIdentifier.setValue("test-1");
        patientIdentifier.setCurrency(PatientIdentifier.Currency.UNKNOWN);

        Object type = Versions.invokeGetMethod(patientIdentifier, "getType");
        List vals = (List) Versions.invokeGetMethod(type, "getCoding");
        System.out.println("TEST-VALS = " + vals);

        assertFalse(IdentifierUtils.checkTypeAndCodingExists(type, vals));
    }

    @Test
    void testR4ExtractIds() throws IOException {
        List<String> beneIds = List.of("-19990000001101", "-19990000001102", "-19990000001103");
        List<String> currentMbis = List.of("3S24A00AA00", "4S24A00AA00", "5S24A00AA00");
        org.hl7.fhir.r4.model.Bundle resource = (org.hl7.fhir.r4.model.Bundle) extractBundle(FhirVersion.R4, "data/r4patients.json");
        for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent component : resource.getEntry()) {
            org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) component.getResource();
            List<PatientIdentifier> ids = IdentifierUtils.getIdentifiers(patient);
            assertEquals(3, ids.size());

            PatientIdentifier benId = IdentifierUtils.getBeneId(ids);
            assertTrue(beneIds.contains(benId.getValue()));
            assertEquals(PatientIdentifier.Type.BENE_ID, benId.getType());
            assertEquals(CURRENT, benId.getCurrency());

            PatientIdentifier mbiCurrentId = IdentifierUtils.getCurrentMbi(ids);
            assertTrue(currentMbis.contains(mbiCurrentId.getValue()));
            assertEquals(CURRENT, mbiCurrentId.getCurrency());

            /* Currently, no historical MBIs are in test data */
            Set<PatientIdentifier> historicalMbi = IdentifierUtils.getHistoricMbi(ids);
            assertEquals(0, historicalMbi.size());
            historicalMbi.forEach(c -> assertTrue(historicalMbi.contains(c)));

           /*
            Set<PatientIdentifier> historicalMbi = IdentifierUtils.getHistoricMbi(ids);
            assertEquals(1, historicalMbi.size());
            PatientIdentifier hist = historicalMbi.stream().findFirst().get();
            assertEquals("111111111", hist.getValue());
            assertEquals(HISTORIC, hist.getCurrency());

            */
        }
    }

    IBaseResource extractBundle(FhirVersion version, String fileName) throws IOException {
        return version.getJsonParser().parseResource(getRawJson(fileName));
    }

    String getRawJson(String path) throws IOException {
        InputStream sampleData =
                PatientIdentifierUtilsTest.class.getClassLoader().getResourceAsStream(path);

        if (sampleData == null) {
            throw new IOException("Cannot find sample requests for path " + path);
        }

        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }
}
