package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EOBLoadUtilitiesTest {
    private static IBaseResource eobC;
    private static IBaseResource eobS;
    private static FhirContext context = FhirContext.forDstu3();

    static {
        eobC = ExplanationOfBenefitTrimmerSTU3
                .getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json"));
        eobS = ExplanationOfBenefitTrimmerSTU3
                .getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-SNF-Claims.json"));
    }

    @Test
    public void testType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<org.hl7.fhir.dstu3.model.Coding> coding = eobCarrier.getType().getCoding();
        assertEquals(4, coding.size());
        org.hl7.fhir.dstu3.model.Coding cd = coding.stream().filter(c -> c.getCode().equals("professional")).findFirst()
                .orElse(null);
        assertNotNull(cd);
        assertEquals("http://hl7.org/fhir/ex-claimtype", cd.getSystem());
        assertEquals("professional", cd.getCode());
        assertEquals("Professional", cd.getDisplay());
    }

    @Test
    public void testResourceType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        assertEquals(org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit, eobCarrier.getResourceType());
    }

    @Test
    public void testDiagnosis() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<ExplanationOfBenefit.DiagnosisComponent> diagnoses = eobCarrier.getDiagnosis();
        assertNotNull(diagnoses);
        assertEquals(5, diagnoses.size());
        ExplanationOfBenefit.DiagnosisComponent comp = diagnoses.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(1, comp.getDiagnosisCodeableConcept().getCoding().size());
        assertEquals("H8888", comp.getDiagnosisCodeableConcept().getCoding().get(0).getCode());
    }

    @Test
    public void testProcedure() throws ParseException {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<ExplanationOfBenefit.ProcedureComponent> procedures = eobSNF.getProcedure();
        assertNotNull(procedures);
        assertEquals(1, procedures.size());
        ExplanationOfBenefit.ProcedureComponent comp = procedures.get(0);
        assertEquals(1, comp.getSequence());
        assertNotNull(comp.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        Date expectedTime = sdf.parse("2016-01-16T00:00:00-0600");
        assertEquals(expectedTime.getTime(), comp.getDate().getTime());
        assertEquals("http://hl7.org/fhir/sid/icd-9-cm",
                comp.getProcedureCodeableConcept().getCoding().get(0).getSystem());
        assertEquals("0TCCCCC", comp.getProcedureCodeableConcept().getCoding().get(0).getCode());
    }

    @Test
    public void testProvider() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getProvider();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("https://bluebutton.cms.gov/resources/variables/prvdr_num", ref.getIdentifier().getSystem());
        assertEquals("299999", ref.getIdentifier().getValue());
    }

    @Test
    public void testOrganization() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getOrganization();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("http://hl7.org/fhir/sid/us-npi", ref.getIdentifier().getSystem());
        assertEquals("1111111111", ref.getIdentifier().getValue());
    }

    @Test
    public void testFacility() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getFacility();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("http://hl7.org/fhir/sid/us-npi", ref.getIdentifier().getSystem());
        assertEquals("1111111111", ref.getIdentifier().getValue());
    }

    @Test
    public void testIdentifier() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<org.hl7.fhir.dstu3.model.Identifier> ids = eobSNF.getIdentifier();
        assertNotNull(ids);
        assertEquals(ids.size(), 2);
        org.hl7.fhir.dstu3.model.Identifier id = ids.stream()
                .filter(c -> c.getValue().equalsIgnoreCase("900"))
                .findFirst().orElse(null);
        assertNotNull(id);
        assertEquals("https://bluebutton.cms.gov/resources/identifier/claim-group", id.getSystem());
    }

    @Test
    public void testCareTeam() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<ExplanationOfBenefit.CareTeamComponent> careTeamComponents = eobSNF.getCareTeam();
        assertNotNull(careTeamComponents);
        assertEquals(careTeamComponents.size(), 4);
        ExplanationOfBenefit.CareTeamComponent comp = careTeamComponents.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals("http://hl7.org/fhir/sid/us-npi", comp.getProvider().getIdentifier().getSystem());
        assertEquals("3333333333", comp.getProvider().getIdentifier().getValue());
        assertEquals("http://hl7.org/fhir/claimcareteamrole", comp.getRole().getCoding().get(0).getSystem());
        assertEquals("assist", comp.getRole().getCoding().get(0).getCode());
        assertEquals("Assisting Provider", comp.getRole().getCoding().get(0).getDisplay());
    }

    @Test
    public void testItems() throws ParseException {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<ExplanationOfBenefit.ItemComponent> components = eobCarrier.getItem();
        assertNotNull(components);
        assertEquals(components.size(), 1);
        assertEquals(2, components.get(0).getCareTeamLinkId().get(0).getValue());
        assertEquals("1", components.get(0).getQuantity().getValue().toString());
        assertEquals(6, components.get(0).getSequence());
        assertEquals("https://bluebutton.cms.gov/resources/codesystem/hcpcs",
                components.get(0).getService().getCoding().get(0).getSystem());
        assertEquals("5", components.get(0).getService().getCoding().get(0).getVersion());
        assertEquals("92999", components.get(0).getService().getCoding().get(0).getCode());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date d = sdf.parse("1999-10-27");
        org.hl7.fhir.dstu3.model.Period period = (org.hl7.fhir.dstu3.model.Period) components.get(0).getServiced();
        assertEquals(period.getStart().getTime(), d.getTime());
        assertEquals(period.getEnd().getTime(), d.getTime());

        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.CodeableConcept location = (org.hl7.fhir.dstu3.model.CodeableConcept) components.get(0)
                .getLocation();
        assertEquals("https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd",
                location.getCoding().get(0).getSystem());
        assertEquals("11", location.getCoding().get(0).getCode());
        assertEquals(
                "Office. Location, other than a hospital, skilled nursing facility (SNF), military treatment facility, community health center, State or local public health clinic, or intermediate care facility (ICF), where the health professional routinely provides health examinations, diagnosis, and treatment of illness or injury on an ambulatory basis.",
                location.getCoding().get(0).getDisplay());

        List<ExplanationOfBenefit.ItemComponent> components2 = eobSNF.getItem();
        org.hl7.fhir.dstu3.model.Address location2 = (org.hl7.fhir.dstu3.model.Address) components2.get(0)
                .getLocation();
        assertEquals("FL", location2.getState());
    }

    @Test
    void testReaderEOB() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-Claims.json")) {
            try (Reader reader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                assertNull(EOBLoadUtilities.getEOBFromReader((Reader) null, context));
                // STU3
                ExplanationOfBenefit benefit = (ExplanationOfBenefit) EOBLoadUtilities.getEOBFromReader(reader,
                        context);
                assertNotNull(benefit);
                assertEquals("Patient/-199900000022040", benefit.getPatient().getReference());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void testToJson() {
        var jsonParser = context.newJsonParser();
        ExplanationOfBenefit eob = EOBLoadUtilities
                .getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json");
        ExplanationOfBenefit eobNew = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerSTU3
                .getBenefit((IBaseResource) eob);
        String payload = jsonParser.encodeResourceToString(eobNew) + System.lineSeparator();
        assertNotNull(payload);
    }

    @Test
    void testNull() {
        ExplanationOfBenefit eob = EOBLoadUtilities.getSTU3EOBFromFileInClassPath("");
        assertNull(eob);
    }
}
