package com.nutrihealth.diagnostic.domain.port.out;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import java.util.List;
import java.util.UUID;

public interface DiagnosticReportRepositoryPort {

    DiagnosticReport save(DiagnosticReport report);

    List<DiagnosticReport> findByUserId(UUID userId);
}
