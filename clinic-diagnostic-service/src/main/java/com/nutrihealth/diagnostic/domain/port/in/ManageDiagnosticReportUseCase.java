package com.nutrihealth.diagnostic.domain.port.in;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import java.util.List;
import java.util.UUID;

public interface ManageDiagnosticReportUseCase {

    List<DiagnosticReport> getReportsForUser(UUID userId);

    /**
     * Ingests a canonical FHIR R4 DiagnosticReport resource (already validated
     * and, where applicable, transformed from legacy HL7 v2) into the domain.
     *
     * @param fhirJsonResource raw JSON body of a FHIR DiagnosticReport resource
     */
    DiagnosticReport ingestFhirDiagnosticReport(String fhirJsonResource);
}
