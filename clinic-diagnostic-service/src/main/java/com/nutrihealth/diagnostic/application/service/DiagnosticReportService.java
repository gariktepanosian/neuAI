package com.nutrihealth.diagnostic.application.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.nutrihealth.diagnostic.domain.model.BiomarkerObservation;
import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.model.InterpretationCode;
import com.nutrihealth.diagnostic.domain.port.in.ManageDiagnosticReportUseCase;
import com.nutrihealth.diagnostic.domain.port.out.DiagnosticReportRepositoryPort;
import com.nutrihealth.diagnostic.domain.port.out.DietaryAdjustmentEventPublisherPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticReportService implements ManageDiagnosticReportUseCase {

    private final DiagnosticReportRepositoryPort repository;
    private final DietaryAdjustmentEventPublisherPort eventPublisher;
    private final FhirContext fhirContext;

    public DiagnosticReportService(DiagnosticReportRepositoryPort repository,
                                    DietaryAdjustmentEventPublisherPort eventPublisher,
                                    FhirContext fhirContext) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.fhirContext = fhirContext;
    }

    @Override
    public List<DiagnosticReport> getReportsForUser(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public DiagnosticReport ingestFhirDiagnosticReport(String fhirJsonResource) {
        IParser jsonParser = fhirContext.newJsonParser();
        org.hl7.fhir.r4.model.DiagnosticReport fhirReport =
                (org.hl7.fhir.r4.model.DiagnosticReport) jsonParser.parseResource(fhirJsonResource);

        UUID userId = extractSubjectUserId(fhirReport);
        UUID clinicId = extractPerformerClinicId(fhirReport);
        Instant testDate = fhirReport.hasEffectiveDateTimeType()
                ? fhirReport.getEffectiveDateTimeType().getValue().toInstant()
                : Instant.now();

        List<BiomarkerObservation> observations = new ArrayList<>();
        for (org.hl7.fhir.r4.model.Reference resultRef : fhirReport.getResult()) {
            if (resultRef.getResource() instanceof Observation observation) {
                observations.add(toBiomarkerObservation(observation));
            }
        }

        DiagnosticReport domainReport = new DiagnosticReport(
                UUID.randomUUID(), userId, clinicId, testDate, observations);

        DiagnosticReport saved = repository.save(domainReport);
        eventPublisher.publishReportIngested(saved);
        return saved;
    }

    private BiomarkerObservation toBiomarkerObservation(Observation observation) {
        String loincCode = observation.getCode().getCodingFirstRep().getCode();
        String display = observation.getCode().getCodingFirstRep().getDisplay();
        double value = observation.getValueQuantity().getValue().doubleValue();
        String unit = observation.getValueQuantity().getUnit();
        InterpretationCode interpretation = observation.hasInterpretation()
                ? InterpretationCode.valueOf(
                        observation.getInterpretationFirstRep().getCodingFirstRep().getCode().toUpperCase())
                : InterpretationCode.NORMAL;
        return new BiomarkerObservation(loincCode, display, value, unit, interpretation);
    }

    private UUID extractSubjectUserId(org.hl7.fhir.r4.model.DiagnosticReport fhirReport) {
        String reference = fhirReport.getSubject().getReference();
        return UUID.fromString(reference.substring(reference.lastIndexOf('/') + 1));
    }

    private UUID extractPerformerClinicId(org.hl7.fhir.r4.model.DiagnosticReport fhirReport) {
        if (fhirReport.getPerformer().isEmpty()) {
            return null;
        }
        String reference = fhirReport.getPerformerFirstRep().getReference();
        return UUID.fromString(reference.substring(reference.lastIndexOf('/') + 1));
    }
}
