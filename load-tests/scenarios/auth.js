import http from 'k6/http';
import { check } from 'k6';

/**
 * Registers (if needed) and logs in a virtual user, returning the JWT token.
 */
export function loginAndGetToken(baseUrl, email, password, latencyMetric, errorRate) {
  // Register (idempotent — ignore 409).
  http.post(`${baseUrl}/api/v1/auth/register`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } });

  const start = Date.now();
  const res = http.post(`${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } });
  latencyMetric.add(Date.now() - start);

  const ok = check(res, {
    'auth: status 200': (r) => r.status === 200,
    'auth: has accessToken': (r) => {
      try { return !!JSON.parse(r.body).accessToken; } catch { return false; }
    },
  });
  errorRate.add(!ok);

  if (!ok) return null;
  return JSON.parse(res.body).accessToken;
}
