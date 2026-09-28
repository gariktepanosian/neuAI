import http from 'k6/http';
import { check } from 'k6';

const SAMPLE_FHIR_JSON = JSON.stringify({
  resourceType: 'DiagnosticReport',
  subject: { reference: 'Patient/loadtest-user' },
  result: [
    {
      reference: '#obs-1',
      resource: {
        resourceType: 'Observation',
        code: { coding: [{ system: 'http://loinc.org', code: '2888-6', display: 'Vitamin D' }] },
        valueQuantity: { value: 25, unit: 'ng/mL' },
        interpretation: [{ coding: [{ code: 'NORMAL' }] }],
      },
    },
  ],
});

export function runDiagnosticsScenario(baseUrl, headers, latencyMetric, errorRate) {
  const start = Date.now();
  const res = http.post(
    `${baseUrl}/api/v1/fhir/diagnostic-report`,
    SAMPLE_FHIR_JSON,
    {
      headers: {
        ...headers,
        'Content-Type': 'application/fhir+json',
        'X-User-Id': `loadtest-${__VU}`,
      },
    }
  );
  latencyMetric.add(Date.now() - start);

  const ok = check(res, {
    'diagnostics: status 201': (r) => r.status === 201,
  });
  errorRate.add(!ok);
}
