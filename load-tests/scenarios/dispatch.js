import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export function runDispatchScenario(baseUrl, headers, latencyMetric, errorRate) {
  // Random delivery location within San Francisco bounds.
  const lat = 37.70 + Math.random() * 0.10;
  const lng = -122.50 + Math.random() * 0.10;

  const start = Date.now();
  const res = http.post(
    `${baseUrl}/api/v1/dispatch`,
    JSON.stringify({
      deliveryId: uuidv4(),
      latitude: lat,
      longitude: lng,
    }),
    { headers }
  );
  latencyMetric.add(Date.now() - start);

  const ok = check(res, {
    'dispatch: status 200 or 404': (r) => r.status === 200 || r.status === 404,
  });
  // 404 is acceptable (no couriers in range) — only 5xx counts as error.
  errorRate.add(res.status >= 500);
}
