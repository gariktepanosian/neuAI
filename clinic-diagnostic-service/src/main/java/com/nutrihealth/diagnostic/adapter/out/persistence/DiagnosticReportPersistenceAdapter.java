package com.nutrihealth.diagnostic.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.diagnostic.adapter.out.encryption.PhiFieldEncryptor;
import com.nutrihealth.diagnostic.domain.model.BiomarkerObservation;
import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.model.InterpretationCode;
import com.nutrihealth.diagnostic.domain.port.out.DiagnosticReportRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticReportPersistenceAdapter implements DiagnosticReportRepositoryPort {

    private final SpringDataDiagnosticReportRepository springDataRepository;
    private final PhiFieldEncryptor phiFieldEncryptor;
    private final ObjectMapper objectMapper;

    public DiagnosticReportPersistenceAdapter(SpringDataDiagnosticReportRepository springDataRepository,
                                               PhiFieldEncryptor phiFieldEncryptor,
                                               ObjectMapper objectMapper) {
        this.springDataRepository = springDataRepository;
        this.phiFieldEncryptor = phiFieldEncryptor;
        this.objectMapper = objectMapper;
    }

    /** Serialization-only shape for the encrypted PHI blob; not exposed outside this adapter. */
    private record ObservationRecord(String loincCode, String display, double value,
                                      String unit, InterpretationCode interpretation) {
    }

    @Override
    public DiagnosticReport save(DiagnosticReport report) {
        try {
            List<ObservationRecord> records = report.getObservations().stream()
                    .map(o -> new ObservationRecord(o.getLoincCode(), o.getDisplay(), o.getValue(),
                            o.getUnit(), o.getInterpretation()))
                    .toList();
            String plaintextJson = objectMapper.writeValueAsString(records);
            String encryptedPayload = phiFieldEncryptor.encrypt(plaintextJson);

            DiagnosticReportDocument document = new DiagnosticReportDocument(
                    report.getId().toString(), report.getUserId(), report.getClinicId(),
                    report.getTestDate(), encryptedPayload);

            springDataRepository.save(document);
            return report;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist diagnostic report", e);
        }
    }

    @Override
    public List<DiagnosticReport> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private DiagnosticReport toDomain(DiagnosticReportDocument document) {
        try {
            String plaintextJson = phiFieldEncryptor.decrypt(document.getEncryptedObservationsPayload());
            ObservationRecord[] records = objectMapper.readValue(plaintextJson, ObservationRecord[].class);
            List<BiomarkerObservation> observations = List.of(records).stream()
                    .map(r -> new BiomarkerObservation(r.loincCode(), r.display(), r.value(), r.unit(), r.interpretation()))
                    .toList();

            return new DiagnosticReport(UUID.fromString(document.getId()), document.getUserId(),
                    document.getClinicId(), document.getTestDate(), observations);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt/deserialize diagnostic report", e);
        }
    }
}
