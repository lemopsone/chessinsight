import http from 'k6/http';
import { check } from 'k6';

const payloadsFile = __ENV.MOVE_PAYLOADS_FILE || '/scripts/move-analysis-payloads.json';
const payloadSource = JSON.parse(open(payloadsFile));
const mode = __ENV.MODE || 'fixed';
const rate = parseInt(__ENV.RPS || '1', 10);
const maxRate = parseInt(__ENV.MAX_RPS || '1', 10);
const duration = __ENV.DURATION || '24h';
const rampStepDuration = __ENV.RAMP_STEP_DURATION || '30s';
const rampHoldDuration = __ENV.RAMP_HOLD_DURATION || '60s';
const preAllocatedVUs = parseInt(__ENV.PRE_ALLOCATED_VUS || '4', 10);
const maxVUs = parseInt(__ENV.MAX_VUS || '64', 10);
const phase = __ENV.PHASE || 'manual';

let movePayload = null;
if (Array.isArray(payloadSource)) {
  if (payloadSource.length !== 1) {
    throw new Error(`MOVE_PAYLOADS_FILE must contain exactly 1 payload, got ${payloadSource.length}`);
  }
  movePayload = payloadSource[0];
} else {
  movePayload = payloadSource;
}

function scenarioConfig() {
  if (mode === 'fixed') {
    return {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs,
      maxVUs,
      gracefulStop: __ENV.GRACEFUL_STOP || '60s'
    };
  }
  if (mode === 'ramping') {
    const safeMaxRate = Math.max(1, maxRate);
    const q1 = Math.max(1, Math.floor(safeMaxRate * 0.25));
    const q2 = Math.max(1, Math.floor(safeMaxRate * 0.5));
    const q3 = Math.max(1, Math.floor(safeMaxRate * 0.75));
    return {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      stages: [
        { target: q1, duration: rampStepDuration },
        { target: q2, duration: rampStepDuration },
        { target: q3, duration: rampStepDuration },
        { target: safeMaxRate, duration: rampStepDuration },
        { target: safeMaxRate, duration: rampHoldDuration }
      ],
      preAllocatedVUs,
      maxVUs,
      gracefulStop: __ENV.GRACEFUL_STOP || '60s'
    };
  }
  throw new Error(`Unsupported MODE: ${mode}. Expected fixed or ramping`);
}

export const options = {
  scenarios: {
    [phase]: scenarioConfig()
  },
  discardResponseBodies: true,
  summaryTrendStats: ['min', 'avg', 'med', 'max', 'p(50)', 'p(75)', 'p(90)', 'p(95)', 'p(99)']
};

export default function () {
  const res = http.post(`${__ENV.BASE_URL}/api/v1/move-evaluations`, JSON.stringify(movePayload), {
    headers: {
      Authorization: `Bearer ${__ENV.TOKEN}`,
      'Content-Type': 'application/json'
    },
    timeout: __ENV.HTTP_TIMEOUT || '60s',
    tags: { phase }
  });

  check(res, {
    'status is 200': (r) => r.status === 200
  }, { phase });
}
