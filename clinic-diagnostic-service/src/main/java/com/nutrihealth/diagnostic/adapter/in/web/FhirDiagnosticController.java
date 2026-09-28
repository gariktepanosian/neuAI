package com.nutrihealth.diagnostic.adapter.in.web;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.port.in.ManageDiagnosticReportUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canonical FHIR ingestion/query surface per the spec's medical data exchange
 * strategy: {@code /fhir/R4/DiagnosticReport} and {@code /fhir/R4/Observation}.
 */
@RestController
public class FhirDiagnosticController {

    private final ManageDiagnosticReportUseCase useCase;

    public FhirDiagnosticController(ManageDiagnosticReportUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(value = "/fhir/R4/DiagnosticReport", consumes = "application/fhir+json")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticReportResponse ingest(@RequestBody String fhirJsonResource) {
        DiagnosticReport report = useCase.ingestFhirDiagnosticReport(fhirJsonResource);
        return DiagnosticReportResponse.from(report);
    }

    @GetMapping(value = "/fhir/R4/DiagnosticReport", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DiagnosticReportResponse> findByUser(@RequestParam UUID userId) {
        return useCase.getReportsForUser(userId).stream()
                .map(DiagnosticReportResponse::from)
                .toList();
    }
}
