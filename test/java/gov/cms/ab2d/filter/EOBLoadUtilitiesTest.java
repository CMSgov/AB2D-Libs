package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EOBLoadUtilitiesTest {
    static org.hl7.fhir.dstu3.model.ExplanationOfBenefit eobCarrier = null;
    static org.hl7.fhir.dstu3.model.ExplanationOfBenefit eobSNF = null;
    static FhirContext context = FhirContext.forDstu3();

    static {
        eobCarrier = ExplanationOfBenefitTrimmerR3.getBenefit(EOBLoadUtilities.getR3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json", context));
        eobSNF = ExplanationOfBenefitTrimmerR3.getBenefit(EOBLoadUtilities.getR3EOBFromFileInClassPath("eobdata/EOB-for-SNF-Claims.json", context));
    }

    @Test
    public void testConvertFromFilePatient() throws IOException {
        assertEquals(eobCarrier.getPatient().getReference(), "Patient/-199900000022040");
    }

    @Test
    public void testLoadFromFilePatient() {
        assertNull(EOBLoadUtilities.getR3EOBFromFileInClassPath("", context));
        assertNull(EOBLoadUtilities.getR3EOBFromFileInClassPath(null, context));
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = EOBLoadUtilities.getR3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json", context);
        assertNotNull(eob);
        assertEquals(eob.getPatient().getReference(), "Patient/-199900000022040");
    }

    @Test
    public void testType() {
        List<org.hl7.fhir.dstu3.model.Coding> coding = eobCarrier.getType().getCoding();
        assertEquals(4, coding.size());
        org.hl7.fhir.dstu3.model.Coding cd = coding.stream().filter(c -> c.getCode().equals("professional")).findFirst().orElse(null);
        assertNotNull(cd);
        assertEquals(cd.getSystem(), "http://hl7.org/fhir/ex-claimtype");
        assertEquals(cd.getCode(), "professional");
        assertEquals(cd.getDisplay(), "Professional");
    }

    @Test
    public void testResourceType() {
        assertEquals(eobCarrier.getResourceType(), org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit);
    }

    @Test
    public void testDiagnosis() {
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent> diagnoses = eobCarrier.getDiagnosis();
        assertNotNull(diagnoses);
        assertEquals(5, diagnoses.size());
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent comp = diagnoses.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(comp.getDiagnosisCodeableConcept().getCoding().size(), 1);
        assertEquals(comp.getDiagnosisCodeableConcept().getCoding().get(0).getCode(), "H8888");
    }

    @Test
    public void testProcedure() throws ParseException {
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent> procedures = eobSNF.getProcedure();
        assertNotNull(procedures);
        assertEquals(procedures.size(), 1);
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent comp = procedures.get(0);
        assertEquals(comp.getSequence(), 1);
        assertNotNull(comp.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date expectedTime = sdf.parse("2016-01-16T00:00:00-0600");
        assertEquals(expectedTime.getTime(), comp.getDate().getTime());
        assertEquals(comp.getProcedureCodeableConcept().getCoding().get(0).getSystem(), "http://hl7.org/fhir/sid/icd-9-cm");
        assertEquals(comp.getProcedureCodeableConcept().getCoding().get(0).getCode(), "0TCCCCC");
    }

    @Test
    public void testProvider() {
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getProvider();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "https://bluebutton.cms.gov/resources/variables/prvdr_num");
        assertEquals(ref.getIdentifier().getValue(), "299999");
    }

    @Test
    public void testOrganization() {
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getOrganization();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(ref.getIdentifier().getValue(), "1111111111");
    }

    @Test
    public void testFacility() {
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getFacility();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(ref.getIdentifier().getValue(), "1111111111");
    }

    @Test
    public void testIdentifier() {
        List<org.hl7.fhir.dstu3.model.Identifier> ids = eobSNF.getIdentifier();
        assertNotNull(ids);
        assertEquals(ids.size(), 2);
        org.hl7.fhir.dstu3.model.Identifier id = ids.stream()
                .filter(c -> c.getValue().equalsIgnoreCase("900"))
                .findFirst().orElse(null);
        assertNotNull(id);
        assertEquals(id.getSystem(), "https://bluebutton.cms.gov/resources/identifier/claim-group");
    }

    @Test
    public void testCareTeam() {
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent> careTeamComponents = eobSNF.getCareTeam();
        assertNotNull(careTeamComponents);
        assertEquals(careTeamComponents.size(), 4);
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent comp = careTeamComponents.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(comp.getProvider().getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(comp.getProvider().getIdentifier().getValue(), "3333333333");
        assertEquals(comp.getRole().getCoding().get(0).getSystem(), "http://hl7.org/fhir/claimcareteamrole");
        assertEquals(comp.getRole().getCoding().get(0).getCode(), "assist");
        assertEquals(comp.getRole().getCoding().get(0).getDisplay(), "Assisting Provider");
    }

    @Test
    public void testItems() throws ParseException {
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent> components = eobCarrier.getItem();
        assertNotNull(components);
        assertEquals(components.size(), 1);
        assertEquals(components.get(0).getCareTeamLinkId().get(0).getValue(), 2);
        assertEquals(components.get(0).getQuantity().getValue().toString(), "1");
        assertEquals(components.get(0).getSequence(), 6);
        assertEquals(components.get(0).getService().getCoding().get(0).getSystem(), "https://bluebutton.cms.gov/resources/codesystem/hcpcs");
        assertEquals(components.get(0).getService().getCoding().get(0).getVersion(), "5");
        assertEquals(components.get(0).getService().getCoding().get(0).getCode(), "92999");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d = sdf.parse("1999-10-27");
        org.hl7.fhir.dstu3.model.Period period = (org.hl7.fhir.dstu3.model.Period) components.get(0).getServiced();
        assertEquals(period.getStart().getTime(), d.getTime());
        assertEquals(period.getEnd().getTime(), d.getTime());

        org.hl7.fhir.dstu3.model.CodeableConcept location = (org.hl7.fhir.dstu3.model.CodeableConcept) components.get(0).getLocation();
        assertEquals(location.getCoding().get(0).getSystem(), "https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd");
        assertEquals(location.getCoding().get(0).getCode(), "11");
        assertEquals(location.getCoding().get(0).getDisplay(), "Office. Location, other than a hospital, skilled nursing facility (SNF), military treatment facility, community health center, State or local public health clinic, or intermediate care facility (ICF), where the health professional routinely provides health examinations, diagnosis, and treatment of illness or injury on an ambulatory basis.");

        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent> components2 = eobSNF.getItem();
        org.hl7.fhir.dstu3.model.Address location2 = (org.hl7.fhir.dstu3.model.Address) components2.get(0).getLocation();
        assertEquals(location2.getState(), "FL");
    }

    @Test
    void testReaderEOB() throws IOException {
        ClassLoader classLoader = EOBLoadUtilities.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-Claims.json");
        Reader reader = new java.io.InputStreamReader(inputStream);
        assertNull(EOBLoadUtilities.getEOBFromReader((Reader) null, context));
        // R3
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit benefit = (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) EOBLoadUtilities.getEOBFromReader(reader, context);
        assertNotNull(benefit);
        assertEquals(benefit.getPatient().getReference(), "Patient/-199900000022040");
    }

    @Test
    void testToJson() {
        var jsonParser = context.newJsonParser();
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = EOBLoadUtilities.getR3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json", context);
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eobNew = ExplanationOfBenefitTrimmerR3.getBenefit(eob);
        String payload = jsonParser.encodeResourceToString(eobNew) + System.lineSeparator();
        assertNotNull(payload);
    }
}