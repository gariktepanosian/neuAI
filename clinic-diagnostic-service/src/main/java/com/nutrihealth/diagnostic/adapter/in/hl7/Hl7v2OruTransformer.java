package com.nutrihealth.diagnostic.adapter.in.hl7;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.parser.PipeParser;
import java.math.BigDecimal;
import java.util.Date;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

/**
 * Legacy compatibility adapter: transforms an inbound HL7 v2 ORU^R01 lab
 * result message into the platform's canonical FHIR R4 DiagnosticReport +
 * Observation resources, per the spec's "Legacy Compatibility" strategy.
 *
 * Intentionally lives in adapter.in (not application) because HL7 v2 is a
 * transport/format concern, not a domain concept.
 */
@Component
public class Hl7v2OruTransformer {

    private final PipeParser pipeParser = new PipeParser();
    private final FhirContext fhirContext;

    public Hl7v2OruTransformer(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * @param rawHl7Message pipe-delimited ORU^R01 message (MSH|...|OBX|...)
     * @param userId        the internal user UUID this HL7 message's patient maps to,
     *                      resolved upstream via the patient identifier crosswalk
     * @return FHIR R4 DiagnosticReport resource, as JSON, ready for
     *         {@link com.nutrihealth.diagnostic.domain.port.in.ManageDiagnosticReportUseCase#ingestFhirDiagnosticReport}
     */
    public String transformToFhirJson(String rawHl7Message, String userId, String clinicId) throws HL7Exception {
        ORU_R01 message = (ORU_R01) pipeParser.parse(rawHl7Message);

        DiagnosticReport fhirReport = new DiagnosticReport();
        fhirReport.setSubject(new Reference("Patient/" + userId));
        fhirReport.setEffective(new org.hl7.fhir.r4.model.DateTimeType(new Date()));
        if (clinicId != null) {
            fhirReport.addPerformer(new Reference("Organization/" + clinicId));
        }

        ORU_R01_PATIENT_RESULT patientResult = message.getPATIENT_RESULT();
        for (int i = 0; i < patientResult.getORDER_OBSERVATIONReps(); i++) {
            ORU_R01_ORDER_OBSERVATION orderObservation = patientResult.getORDER_OBSERVATION(i);
            for (int j = 0; j < orderObservation.getOBSERVATIONReps(); j++) {
                ORU_R01_OBSERVATION obsGroup = orderObservation.getOBSERVATION(j);
                OBX obx = obsGroup.getOBX();
                Observation observation = toFhirObservation(obx);
                Reference resultRef = new Reference();
                resultRef.setResource(observation);
                fhirReport.addResult(resultRef);
            }
        }

        return fhirContext.newJsonParser().encodeResourceToString(fhirReport);
    }

    private Observation toFhirObservation(OBX obx) throws HL7Exception {
        Observation observation = new Observation();

        String loincCode = obx.getObservationIdentifier().getIdentifier().getValue();
        String display = obx.getObservationIdentifier().getText().getValue();
        observation.setCode(new CodeableConcept().addCoding(
                new org.hl7.fhir.r4.model.Coding("http://loinc.org", loincCode, display)));

        String rawValue = obx.getObservationValue(0).getData().toString();
        String unit = obx.getUnits().getIdentifier().getValue();
        observation.setValue(new Quantity().setValue(new BigDecimal(rawValue)).setUnit(unit));

        String abnormalFlag = obx.getAbnormalFlags(0).getValue();
        observation.addInterpretation(new CodeableConcept().addCoding(
                new org.hl7.fhir.r4.model.Coding().setCode(mapAbnormalFlag(abnormalFlag))));

        return observation;
    }

    /** Maps HL7 v2 Table 0078 abnormal flags to the domain's interpretation codes. */
    private String mapAbnormalFlag(String hl7Flag) {
        if (hl7Flag == null) {
            return "NORMAL";
        }
        return switch (hl7Flag) {
            case "L", "LL" -> "LOW";
            case "H", "HH" -> "HIGH";
            case "PANIC" -> "CRITICAL";
            default -> "NORMAL";
        };
    }
}
