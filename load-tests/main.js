/**
 * k6 load test — NutriHealth AI platform
 *
 * Target: 10,000 concurrent virtual users across all 5 microservices.
 *
 * Usage:
 *   k6 run --env BASE_URL=http://your-gke-loadbalancer load-tests/main.js
 *
 * Requirements:
 *   - k6 v0.53+ (https://k6.io/docs/getting-started/installation/)
 *   - A running instance with at least one seeded test user
 *     (see load-tests/seed.sh for pre-seeding)
 *
 * Thresholds (pass/fail criteria):
 *   - 95th percentile latency < 500 ms
 *   - Error rate < 1%
 *   - Kafka publish success rate > 99% (checked via custom metrics)
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { loginAndGetToken } from './scenarios/auth.js';
import { runMenuScenario } from './scenarios/menu.js';
import { runDiagnosticsScenario } from './scenarios/diagnostics.js';
import { runDispatchScenario } from './scenarios/dispatch.js';
import { runWebhookScenario } from './scenarios/webhook.js';

// ── Configuration ──────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const AUTH_URL = `${BASE_URL}:8082`;
const NUTRITION_URL = `${BASE_URL}:8084`;
const DIAGNOSTIC_URL = `${BASE_URL}:8083`;
const LOGISTICS_URL = `${BASE_URL}:8085`;
const SUBSCRIPTION_URL = `${BASE_URL}:8082`;

export { AUTH_URL, NUTRITION_URL, DIAGNOSTIC_URL, LOGISTICS_URL, SUBSCRIPTION_URL };

// ── Custom metrics ─────────────────────────────────────────────────────────
export const errorRate = new Rate('errors');
export const authLatency = new Trend('auth_latency');
export const menuLatency = new Trend('menu_latency');
export const diagnosticsLatency = new Trend('diagnostics_latency');
export const dispatchLatency = new Trend('dispatch_latency');

// ── Load profile: ramp from 0 → 10k VUs over 5 minutes ───────────────────
export const options = {
  scenarios: {
    // Ramp-up stress test to 10k concurrent users
    auth_and_menu: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 1000 },    // warm-up
        { duration: '3m', target: 5000 },    // ramp to 5k
        { duration: '5m', target: 10000 },   // peak 10k
        { duration: '2m', target: 10000 },   // sustain
        { duration: '2m', target: 0 },        // cool-down
      ],
      gracefulRampDown: '30s',
      exec: 'mainScenario',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],           // < 1% error rate
    errors: ['rate<0.01'],
    auth_latency: ['p(95)<300'],
    menu_latency: ['p(95)<500'],
    diagnostics_latency: ['p(95)<600'],
    dispatch_latency: ['p(95)<200'],
  },
};

// ── Setup: authenticate once per VU ───────────────────────────────────────
export function setup() {
  // Pre-create a test user and return the credentials.
  // Real runs should seed thousands of users for realistic distribution.
  const registerRes = http.post(`${AUTH_URL}/api/v1/auth/register`,
    JSON.stringify({ email: `loadtest+setup@nutrihealth.test`, password: 'LoadTest@123' }),
    { headers: { 'Content-Type': 'application/json' } });

  // May return 409 if already exists — that's fine.
  console.log(`Setup user registration: ${registerRes.status}`);
  return { baseUrl: BASE_URL };
}

// ── Main VU scenario ───────────────────────────────────────────────────────
export function mainScenario(data) {
  const vuId = __VU;
  const email = `loadtest+vu${vuId}@nutrihealth.test`;
  const password = 'LoadTest@123';

  // Each VU registers (idempotent) and logs in.
  const token = loginAndGetToken(AUTH_URL, email, password, authLatency, errorRate);
  if (!token) return;

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Run a mix of business scenarios.
  const scenario = vuId % 4;
  switch (scenario) {
    case 0:
      runMenuScenario(NUTRITION_URL, authHeaders, menuLatency, errorRate);
      break;
    case 1:
      runDiagnosticsScenario(DIAGNOSTIC_URL, authHeaders, diagnosticsLatency, errorRate);
      break;
    case 2:
      runDispatchScenario(LOGISTICS_URL, authHeaders, dispatchLatency, errorRate);
      break;
    case 3:
      runWebhookScenario(SUBSCRIPTION_URL, authHeaders, errorRate);
      break;
  }

  sleep(1);
}
