import http from 'k6/http';
import { check } from 'k6';

export function runMenuScenario(baseUrl, headers, latencyMetric, errorRate) {
  const targetCalories = 1500 + Math.floor(Math.random() * 1000);
  const start = Date.now();
  const res = http.get(
    `${baseUrl}/api/v1/menu?targetCalories=${targetCalories}`,
    { headers }
  );
  latencyMetric.add(Date.now() - start);

  const ok = check(res, {
    'menu: status 200': (r) => r.status === 200,
    'menu: returns array': (r) => {
      try { return Array.isArray(JSON.parse(r.body)); } catch { return false; }
    },
  });
  errorRate.add(!ok);
}
