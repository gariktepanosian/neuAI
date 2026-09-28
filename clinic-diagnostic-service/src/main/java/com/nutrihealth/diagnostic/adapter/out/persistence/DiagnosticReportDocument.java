package com.nutrihealth.diagnostic.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo representation of a diagnostic report. Only non-sensitive routing
 * metadata (ids, dates) is stored in the clear; the observation payload is
 * PHI and is kept as an opaque AES-256-GCM ciphertext blob, never as
 * structured JSON, per the spec's field-level PHI encryption requirement.
 */
@Document(collection = "health_profiles")
public class DiagnosticReportDocument {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private UUID clinicId;
    private Instant testDate;

    /** base64(IV):base64(ciphertext) produced by PhiFieldEncryptor — opaque, never parsed as JSON at rest. */
    private String encryptedObservationsPayload;

    public DiagnosticReportDocument() {
    }

    public DiagnosticReportDocument(String id, UUID userId, UUID clinicId, Instant testDate,
                                     String encryptedObservationsPayload) {
        this.id = id;
        this.userId = userId;
        this.clinicId = clinicId;
        this.testDate = testDate;
        this.encryptedObservationsPayload = encryptedObservationsPayload;
    }

    public String getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getClinicId() {
        return clinicId;
    }

    public Instant getTestDate() {
        return testDate;
    }

    public String getEncryptedObservationsPayload() {
        return encryptedObservationsPayload;
    }
}
