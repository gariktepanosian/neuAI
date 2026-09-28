package com.nutrihealth.diagnostic.adapter.in.hl7;

import com.nutrihealth.diagnostic.adapter.in.web.DiagnosticReportResponse;
import com.nutrihealth.diagnostic.domain.port.in.ManageDiagnosticReportUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for legacy partner labs still emitting HL7 v2 ORU^R01
 * messages instead of FHIR. Transforms then delegates to the same domain
 * use case as the native FHIR endpoint, so downstream logic never branches
 * on source format.
 */
@RestController
public class Hl7v2IngestController {

    private final Hl7v2OruTransformer transformer;
    private final ManageDiagnosticReportUseCase useCase;

    public Hl7v2IngestController(Hl7v2OruTransformer transformer, ManageDiagnosticReportUseCase useCase) {
        this.transformer = transformer;
        this.useCase = useCase;
    }

    @PostMapping(value = "/hl7v2/oru", consumes = "text/plain")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticReportResponse ingestOru(@RequestBody String rawHl7Message,
                                               @RequestParam String userId,
                                               @RequestParam(required = false) String clinicId) throws Exception {
        String fhirJson = transformer.transformToFhirJson(rawHl7Message, userId, clinicId);
        return DiagnosticReportResponse.from(useCase.ingestFhirDiagnosticReport(fhirJson));
    }
}
