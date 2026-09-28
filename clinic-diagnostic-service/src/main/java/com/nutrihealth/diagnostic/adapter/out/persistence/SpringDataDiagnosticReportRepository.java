package com.nutrihealth.diagnostic.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataDiagnosticReportRepository extends MongoRepository<DiagnosticReportDocument, String> {

    List<DiagnosticReportDocument> findByUserId(UUID userId);
}
