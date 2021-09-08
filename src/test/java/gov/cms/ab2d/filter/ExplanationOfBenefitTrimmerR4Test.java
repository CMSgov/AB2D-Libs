package gov.cms.ab2d.filter;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.VisionPrescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome.COMPLETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExplanationOfBenefitTrimmerR4Test {
    private static ExplanationOfBenefit eob;
    private static final Date SAMPLE_DATE = new Date();
    private static final String DUMMY_REF = "1234";
    private static final String DUMMY_TYPE = "type";
    private static final String DUMMY_TXT = "text";

    @BeforeEach
    void initFullEob() {
        // Populate every sub-object of ExplanationOfBenefit
        eob = new ExplanationOfBenefit();
        eob.setText(new Narrative().setStatus(Narrative.NarrativeStatus.ADDITIONAL));
        eob.setContained(List.of(eob));
        eob.setMeta(new Meta().setLastUpdated(SAMPLE_DATE));
        eob.setPatient(new Reference("Patient/1234"));
        eob.setPatientTarget(new Patient().setBirthDate(SAMPLE_DATE));
        eob.setCreated(SAMPLE_DATE);
        eob.setEnterer(new Reference(DUMMY_REF));
        eob.setEntererTarget(new Patient().setBirthDate(SAMPLE_DATE));
        eob.setInsurer(new Reference(DUMMY_REF));
        eob.setInsurerTarget(new Organization().setName("Org"));
        eob.setProvider(new Reference(DUMMY_REF));
        eob.setProviderTarget(new Organization().setName("Org"));
        eob.setReferral(new Reference(DUMMY_REF));
        ServiceRequest req = new ServiceRequest();
        req.setId("Ref");
        eob.setReferralTarget(req);
        eob.setFacility(new Reference(DUMMY_REF));
        eob.setFacilityTarget(new Location().setName("Facility"));
        eob.setClaim(new Reference(DUMMY_REF));
        eob.setClaimTarget(new Claim().setCreated(SAMPLE_DATE));
        eob.setClaimResponse(new Reference(DUMMY_REF));
        eob.setClaimResponseTarget(new ClaimResponse().setCreated(SAMPLE_DATE));
        eob.setDisposition("Disposition");
        eob.setPrescription(new Reference(DUMMY_REF));
        eob.setPrescriptionTarget(new VisionPrescription().setCreated(SAMPLE_DATE));
        eob.setOriginalPrescription(new Reference(DUMMY_REF));
        eob.setOriginalPrescriptionTarget(new MedicationRequest().setAuthoredOn(SAMPLE_DATE));
        eob.setPrecedence(2);
        eob.setPriority(new CodeableConcept().setText(DUMMY_TXT));
        eob.setFundsReserveRequested(new CodeableConcept().setText(DUMMY_TXT));
        eob.setFundsReserve(new CodeableConcept().setText(DUMMY_TXT));
        eob.setPreAuthRef(List.of(new StringType("preauth")));
        eob.setPreAuthRefPeriod(List.of(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE)));
        eob.setFormCode(new CodeableConcept().setText(DUMMY_TXT));
        eob.setBenefitPeriod(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE));
        eob.setOutcome(COMPLETE);
        eob.setRelated(List.of(new ExplanationOfBenefit.RelatedClaimComponent().setReference(new Identifier().setValue("one"))));
        eob.setPayee(new ExplanationOfBenefit.PayeeComponent().setParty(new Reference("party")));
        eob.setInsurance(List.of(new ExplanationOfBenefit.InsuranceComponent().setCoverage(new Reference("coverage"))));
        eob.setAccident(new ExplanationOfBenefit.AccidentComponent().setDate(SAMPLE_DATE));
        eob.setAddItem(List.of(new ExplanationOfBenefit.AddedItemComponent().setUnitPrice(new Money().setValue(10))));
        eob.setPayment(new ExplanationOfBenefit.PaymentComponent().setDate(SAMPLE_DATE));
        eob.setForm(new Attachment().setCreation(SAMPLE_DATE));
        eob.setProcessNote(List.of(new ExplanationOfBenefit.NoteComponent().setType(Enumerations.NoteType.DISPLAY)));
        eob.setBenefitBalance(List.of(new ExplanationOfBenefit.BenefitBalanceComponent().setDescription("Desc")));
        eob.setTotal(List.of(new ExplanationOfBenefit.TotalComponent().setAmount(new Money().setValue(13))));
        eob.setUse(ExplanationOfBenefit.Use.CLAIM);
        eob.setAdjudication(List.of(new ExplanationOfBenefit.AdjudicationComponent().setAmount(new Money().setValue(11))));
        eob.setSupportingInfo(List.of(new ExplanationOfBenefit.SupportingInformationComponent().setSequence(3).setReason(new Coding().setCode("code"))));
        eob.setItem(List.of(new ExplanationOfBenefit.ItemComponent().setSequence(3).setCategory(new CodeableConcept().setText("category"))));
        eob.setIdentifier(List.of(new Identifier().setType(new CodeableConcept().setText("one")).setValue("value")));
        eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED);
        eob.setType(new CodeableConcept().setText(DUMMY_TYPE));
        eob.setSubType(new CodeableConcept().setText("subtype"));
        eob.setBillablePeriod(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE));

        eob.setCareTeam(List.of(
                new ExplanationOfBenefit.CareTeamComponent().setResponsible(true)
                .setRole(new CodeableConcept().setText("care"))
                .setProvider(new Reference("provider"))
        ));
        eob.setDiagnosis(List.of(
                new ExplanationOfBenefit.DiagnosisComponent()
                .setSequence(1)
                .setOnAdmission(new CodeableConcept().setText("admission"))
                .setType(List.of(new CodeableConcept().setText(DUMMY_TYPE)))
        ));
        eob.setProcedure(List.of(
                new ExplanationOfBenefit.ProcedureComponent()
                .setType(List.of(new CodeableConcept().setText(DUMMY_TYPE)))
                .setUdi(List.of(new Reference("udi")))
                .setProcedure(new CodeableConcept().setText("procedure"))
                .setDate(SAMPLE_DATE)
        ));
    }

    /**
     * Verify that all the data that is not available to the PDP is not in the filtered object
     */
    @Test
    void testFilterIt() {
        ExplanationOfBenefit eobtrim = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        assertEquals(Narrative.NarrativeStatus.ADDITIONAL, eobtrim.getText().getStatus());
        assertEquals(0, eobtrim.getExtension().size());
        assertEquals(0, eobtrim.getContained().size());
        assertEquals(SAMPLE_DATE, eobtrim.getMeta().getLastUpdated());
        assertEquals("Patient/1234", eobtrim.getPatient().getReference());
        assertNull(eobtrim.getPatientTarget().getBirthDate());
        assertNull(eobtrim.getCreated());
        assertNull(eobtrim.getEnterer().getReference());
        assertNull(eobtrim.getEntererTarget());
        assertNull(eobtrim.getInsurer().getReference());
        assertNull(eobtrim.getInsurerTarget().getName());
        assertEquals(DUMMY_REF, eobtrim.getProvider().getReference());
        assertNull(eobtrim.getProviderTarget());
        assertNull(eobtrim.getReferral().getReference());
        assertNull(eobtrim.getReferralTarget().getId());
        assertEquals(DUMMY_REF, eobtrim.getFacility().getReference());
        assertNull(eobtrim.getFacilityTarget().getName());
        assertNull(eobtrim.getClaim().getReference());
        assertNull(eobtrim.getClaimTarget().getCreated());
        assertNull(eobtrim.getClaimResponse().getReference());
        assertNull(eobtrim.getClaimResponseTarget().getCreated());
        assertNull(eobtrim.getDisposition());
        assertNull(eobtrim.getPrescription().getReference());
        assertNull(eobtrim.getPrescriptionTarget());
        assertNull(eobtrim.getOriginalPrescription().getReference());
        assertNull(eobtrim.getOriginalPrescriptionTarget().getAuthoredOn());
        assertEquals(0, eobtrim.getPrecedence());
        assertNull(eobtrim.getPriority().getText());
        assertNull(eobtrim.getFundsReserveRequested().getText());
        assertNull(eobtrim.getFundsReserve().getText());
        assertEquals(0, eobtrim.getPreAuthRef().size());
        assertEquals(0, eobtrim.getPreAuthRefPeriod().size());
        assertNull(eobtrim.getFormCode().getText());
        assertNull(eobtrim.getBenefitPeriod().getStart());
        assertNull(eobtrim.getBenefitPeriod().getEnd());
        assertNull(eobtrim.getOutcome());
        assertEquals(0, eobtrim.getRelated().size());
        assertNull(eobtrim.getPayee().getParty().getReference());
        assertEquals(0, eobtrim.getInsurance().size());
        assertNull(eobtrim.getAccident().getDate());
        assertEquals(0, eobtrim.getAddItem().size());
        assertNull(eobtrim.getPayment().getDate());
        assertNull(eobtrim.getForm().getCreation());
        assertEquals(0, eobtrim.getProcessNote().size());
        assertEquals(0, eobtrim.getBenefitBalance().size());
        assertEquals(0, eobtrim.getTotal().size());
        assertNull(eobtrim.getUse());
        assertEquals(0, eobtrim.getAdjudication().size());
        assertEquals(0, eobtrim.getSupportingInfo().size());
        assertEquals(1, eobtrim.getItem().size());
        ExplanationOfBenefit.ItemComponent component = eobtrim.getItem().get(0);
        assertNull(component.getCategory().getText());
        assertEquals(3, component.getSequence());
        assertEquals(1, eobtrim.getIdentifier().size());
        Identifier id = eobtrim.getIdentifier().get(0);
        assertEquals("value", id.getValue());
        assertEquals("one", id.getType().getText());
        assertEquals(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED, eobtrim.getStatus());
        assertEquals(DUMMY_TYPE, eobtrim.getType().getText());
        assertEquals("subtype", eobtrim.getSubType().getText());
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  eobtrim.getBillablePeriod().getStart().getTime()) < 1000);
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  eobtrim.getBillablePeriod().getEnd().getTime()) < 1000);
        assertEquals(1, eobtrim.getCareTeam().size());
        ExplanationOfBenefit.CareTeamComponent careTeamComponent = eobtrim.getCareTeamFirstRep();
        assertTrue(careTeamComponent.getResponsible());
        assertEquals("care", careTeamComponent.getRole().getText());
        assertEquals("provider", careTeamComponent.getProvider().getReference());
        assertEquals(1, eobtrim.getDiagnosis().size());
        ExplanationOfBenefit.DiagnosisComponent diagnosisComponent = eobtrim.getDiagnosisFirstRep();
        assertEquals(1, diagnosisComponent.getSequence());
        assertNull(diagnosisComponent.getOnAdmission().getText());
        assertEquals(DUMMY_TYPE, diagnosisComponent.getType().get(0).getText());
        assertEquals(1, eobtrim.getProcedure().size());
        ExplanationOfBenefit.ProcedureComponent procedureComponent = eobtrim.getProcedureFirstRep();
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  procedureComponent.getDate().getTime()) < 1000);
        assertEquals(0, procedureComponent.getType().size());
        assertEquals(0, procedureComponent.getUdi().size());
        assertEquals("procedure", ((CodeableConcept) procedureComponent.getProcedure()).getText());
    }
}
