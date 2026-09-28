package com.nutrihealth.diagnostic.adapter.in.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import com.nutrihealth.diagnostic.domain.port.in.ManageDiagnosticReportUseCase;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HAPI {@link ReceivingApplication} that handles ORU^R01 messages arriving
 * over the MLLP TCP transport ({@link MllpInboundServer}).
 *
 * <p>The handler:
 * <ol>
 *   <li>Extracts the patient identifier (PID-3 or PID-18) from the HL7 message
 *       and maps it to a NutriHealth userId via a future patient-identifier
 *       crosswalk (for now the PID-3.1 value is used directly as userId).</li>
 *   <li>Delegates to {@link Hl7v2OruTransformer} to convert the HL7 message to a
 *       FHIR R4 DiagnosticReport JSON string.</li>
 *   <li>Calls the domain use-case to persist and publish the report.</li>
 *   <li>Returns an ACK (Application ACK) message so the sending lab system knows
 *       the message was accepted.</li>
 * </ol>
 *
 * <p>Errors (HL7 parse failures, domain exceptions) result in an AE
 * (Application Error) ACK being returned to the sender, ensuring the lab system
 * retries or routes to its own DLQ.
 */
@Component
public class MllpOruR01Handler implements ReceivingApplication<Message> {

    private static final Logger log = LoggerFactory.getLogger(MllpOruR01Handler.class);

    private final Hl7v2OruTransformer transformer;
    private final ManageDiagnosticReportUseCase useCase;

    public MllpOruR01Handler(Hl7v2OruTransformer transformer, ManageDiagnosticReportUseCase useCase) {
        this.transformer = transformer;
        this.useCase = useCase;
    }

    @Override
    public Message processMessage(Message message, Map<String, Object> metadata)
            throws ReceivingApplicationException, HL7Exception {
        String rawHl7 = message.encode();
        log.debug("MLLP received ORU^R01 ({} bytes)", rawHl7.length());

        try {
            // Extract patient ID from PID segment (PID-3.1 = patient identifier list)
            String userId = extractUserId(message);
            String clinicId = extractClinicId(message);

            String fhirJson = transformer.transformToFhirJson(rawHl7, userId, clinicId);
            useCase.ingestFhirDiagnosticReport(fhirJson);

            log.info("MLLP ORU^R01 ingested successfully userId={}", userId);
            return message.generateACK(); // AA — Application Accept
        } catch (Exception ex) {
            log.error("MLLP ORU^R01 processing failed: {}", ex.getMessage(), ex);
            throw new ReceivingApplicationException(
                    "Failed to process ORU^R01 message: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean canProcess(Message message) {
        try {
            return "ORU".equals(message.getName().split("_")[0])
                    && "R01".equals(message.getName().split("_")[1]);
        } catch (Exception e) {
            return false;
        }
    }

    /** Extracts PID-3.1 (patient identifier) as userId. */
    private String extractUserId(Message message) throws HL7Exception {
        ca.uhn.hl7v2.model.v25.segment.PID pid =
                (ca.uhn.hl7v2.model.v25.segment.PID) message.get("PATIENT_RESULT/PATIENT/PID");
        String patientId = pid.getPatientIdentifierList(0).getIDNumber().getValue();
        return patientId != null ? patientId : "unknown";
    }

    /** Extracts MSH-4.1 (sending facility) as clinicId. */
    private String extractClinicId(Message message) throws HL7Exception {
        ca.uhn.hl7v2.model.v25.segment.MSH msh =
                (ca.uhn.hl7v2.model.v25.segment.MSH) message.get("MSH");
        return msh.getSendingFacility().getNamespaceID().getValue();
    }
}
