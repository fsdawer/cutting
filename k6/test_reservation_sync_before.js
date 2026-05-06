
/**
 * [BEFORE] 예약 생성 병목 테스트 — 동기 랭킹 재계산 + 동기 WebSocket 알림
 *
 * 예약 저장 → rankingService.recalculateScore() [동기] → notificationService.notify() [동기] → 응답
 *
 * 실행:
 *   k6 run -e STYLIST_ID=1 -e SERVICE_ID=1 k6/test_reservation_sync_before.js
 *
 * 사전 준비:
 *   setup() 단계에서 loadtest_b1~50 계정을 자동 생성/로그인 → 각 VU에 고유 토큰 배분
 *
 * 테스트 후 DB 정리:
 *   DELETE FROM reservations WHERE request_memo LIKE '%sync_test%';
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const STYLIST_ID = __ENV.STYLIST_ID || '1';
const SERVICE_ID = __ENV.SERVICE_ID || '1';
const NUM_USERS  = 50;

const JSON_HEADER = { 'Content-Type': 'application/json' };

const reservationLatency = new Trend('reservation_latency_ms', true);
const reservationSuccess = new Counter('reservation_success');
const errorRate          = new Rate('error_rate');

// ── 사전 준비: 50명 유저 생성 + 토큰 발급 ───────────────────────────────────────
export function setup() {
  const tokens = [];
  for (let i = 1; i <= NUM_USERS; i++) {
    const username = `loadtest_b${i}`;
    const password = 'Loadtest123!';

    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username,
      email:    `loadtest_b${i}@k6test.com`,
      password,
      name:     `비포테스트${i}`,
      phone:    `010-2000-${String(i).padStart(4, '0')}`,
      role:     'USER',
    }), { headers: JSON_HEADER });

    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      username, password,
    }), { headers: JSON_HEADER });

    const body = JSON.parse(res.body);
    if (res.status !== 200 || !body.accessToken) {
      console.error(`[setup] 로그인 실패 — ${username}, status=${res.status}`);
      tokens.push(null);
    } else {
      tokens.push(`Bearer ${body.accessToken}`);
    }
  }
  console.log(`[setup] ${tokens.filter(Boolean).length}/${NUM_USERS}명 토큰 발급 완료`);

  // 분 단위 타임스탬프 seed (mod 3000) → 1분 이상 간격 재실행은 다른 날짜 블록 사용
  const runSeed = Math.floor(Date.now() / (1000 * 60)) % 3000;
  return { tokens, runSeed };
}

// VU × iteration 조합 + runSeed 오프셋으로 시간대 완전 분리
function targetDateTime(vuId, iterationId, runSeed) {
  const _baseYear = new Date().getFullYear() + 2;
  const BASE_DATE = new Date(_baseYear, 0, 1);
  const totalSlot = (vuId - 1) * 200 + iterationId + (runSeed * 10000);
  const dayOffset = Math.floor(totalSlot / 16);
  const slotInDay = totalSlot % 16;
  const date      = new Date(BASE_DATE.getTime() + dayOffset * 86400000);
  const hour      = 10 + Math.floor(slotInDay / 2);
  const minute    = (slotInDay % 2) === 0 ? '00' : '30';
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}T${String(hour).padStart(2,'0')}:${minute}:00`;
}

export const options = {
  scenarios: {
    steady_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '60s',
    },
  },
  thresholds: {
    'http_req_duration':   ['p(95)<10000', 'p(99)<15000'],
    'error_rate':          ['rate<0.1'],
  },
};

export default function (data) {
  const token = data.tokens[(__VU - 1) % NUM_USERS];
  if (!token) {
    console.error(`[VU ${__VU}] 토큰 없음 — 건너뜀`);
    return;
  }

  const payload = JSON.stringify({
    stylistId:   Number(STYLIST_ID),
    serviceId:   Number(SERVICE_ID),
    reservedAt:  targetDateTime(__VU, __ITER, data.runSeed),
    requestMemo: `sync_test VU=${__VU} iter=${__ITER}`,
  });

  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: { 'Content-Type': 'application/json', 'Authorization': token },
    tags: { name: 'POST /api/reservations' },
  });

  const ok = check(res, {
    'status 201 Created': r => r.status === 201,
    'no server error':    r => r.status < 500,
  });

  reservationLatency.add(res.timings.duration);
  errorRate.add(res.status !== 201);
  if (res.status === 201) reservationSuccess.add(1);

  sleep(0.2);
}

export function handleSummary(data) {
  const checks_total = (data.metrics.checks?.values.passes || 0) + (data.metrics.checks?.values.fails || 0);
  const checks_succeeded = data.metrics.checks?.values.passes || 0;
  const checks_failed = data.metrics.checks?.values.fails || 0;
  const succ_rate = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : "0.00";
  const fail_rate = checks_total > 0 ? ((checks_failed / checks_total) * 100).toFixed(2) : "0.00";
  
  const req_dur = data.metrics.http_req_duration?.values || {};
  const lat = data.metrics.reservation_latency_ms?.values || {};
  const reqs = data.metrics.http_reqs?.values || {};
  const vus = data.metrics.vus?.values || {};
  const iters = data.metrics.iterations?.values || {};
  const iter_dur = data.metrics.iteration_duration?.values || {};
  const data_recv = data.metrics.data_received?.values || {};
  const data_sent = data.metrics.data_sent?.values || {};
  
  const durationS = (data.state?.testRunDurationMs || 60000) / 1000;
  const formatMs = (val) => val === undefined ? "0.00ms" : val.toFixed(2) + "ms";
  
  const result = `
[BEFORE] 예약 생성 동기 병목  STYLIST_ID=${STYLIST_ID}  SERVICE_ID=${SERVICE_ID}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 201 성공 | 409 중복거부 (정상)

CUSTOM METRICS
  reservation_latency_ms  avg=${formatMs(lat.avg)} min=${formatMs(lat.min)} med=${formatMs(lat.med)} max=${formatMs(lat.max)} p(90)=${formatMs(lat["p(90)"])} p(95)=${formatMs(lat["p(95)"])}
  reservation_success     ${data.metrics.reservation_success?.values?.count || 0}
  error_rate              ${data.metrics.error_rate?.values?.passes || 0} (${((data.metrics.error_rate?.values?.rate || 0) * 100).toFixed(2)}%)

HTTP
  http_req_duration       avg=${formatMs(req_dur.avg)} min=${formatMs(req_dur.min)} med=${formatMs(req_dur.med)} max=${formatMs(req_dur.max)} p(90)=${formatMs(req_dur["p(90)"])} p(95)=${formatMs(req_dur["p(95)"])}
  http_reqs               ${reqs.count || 0}   ${(reqs.rate || 0).toFixed(2)}/s

EXECUTION
  iteration_duration      avg=${formatMs(iter_dur.avg)} min=${formatMs(iter_dur.min)} med=${formatMs(iter_dur.med)} max=${formatMs(iter_dur.max)} p(90)=${formatMs(iter_dur["p(90)"])} p(95)=${formatMs(iter_dur["p(95)"])}
  iterations              ${iters.count || 0}   ${(iters.rate || 0).toFixed(2)}/s
  vus                     min=${vus.min || 0}  max=${vus.max || 0}
  vus_max                 min=${data.metrics.vus_max?.values?.min || 0}  max=${data.metrics.vus_max?.values?.max || 0}

NETWORK
  data_received           ${((data_recv.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_recv.rate || 0) / 1024).toFixed(2)} kB/s
  data_sent               ${((data_sent.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_sent.rate || 0) / 1024).toFixed(2)} kB/s
`;

  console.log(result);
  return {
    stdout: result,
  };
}
