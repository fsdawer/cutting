/**
 * 병목 3: POST /api/reservations — 동시 예약 레이스 컨디션
 *
 * Before: Redis 락 없음 → 동시 요청이 같은 시간대에 중복 예약 생성 가능
 * After:  Redis SETNX 분산 락 → 1건만 성공, 나머지는 409
 *
 * 실행:
 *   k6 run -e STYLIST_ID=1 -e SERVICE_ID=1 k6/test_distributed_lock.js
 *
 * 사전 준비:
 *   setup() 단계에서 loadtest_b1~50 계정을 자동 생성/로그인 → 각 VU에 고유 토큰 배분
 *
 * 결과 해석:
 *   Before: http_req_status_201 > 1 이면 중복 예약 발생 (버그 확인)
 *   After:  http_req_status_201 = 1, http_req_status_409 = 나머지 (정상)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const STYLIST_ID = __ENV.STYLIST_ID || '1';
const SERVICE_ID = __ENV.SERVICE_ID || '1';
const NUM_USERS  = 50;

const JSON_HEADER = { 'Content-Type': 'application/json' };

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
  return { tokens };
}

const successCount  = new Counter('reservation_created');   // 201 카운트
const conflictCount = new Counter('reservation_conflict');  // 409 카운트
const errorRate     = new Rate('unexpected_error_rate');    // 201/409 외 응답

// 30일 후 랜덤 시간 — 매 실행마다 겹치지 않는 신선한 슬롯 사용
// (초 단위로 유니크하게 → 반복 실행 시에도 이전 예약과 충돌 없음)
function targetTime() {
  const d = new Date();
  const seed = Math.floor(Date.now() / 1000);   // 초 단위 고유값
  d.setDate(d.getDate() + 30 + (seed % 30));    // 30~59일 후
  const hour = 10 + (seed % 8);                  // 10~17시
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}T${String(hour).padStart(2, '0')}:00:00`;
}

export const options = {
  scenarios: {
    // 50명이 동시에 같은 시간대 예약 시도 — 레이스 컨디션 재현
    concurrent_booking: {
      executor: 'shared-iterations',
      vus: 50,
      iterations: 50,
      maxDuration: '15s',
    },
  },
  thresholds: {
    // After 기준: 예상치 못한 오류(500 등)는 없어야 함
    unexpected_error_rate: ['rate<0.01'],
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
    reservedAt:  targetTime(),
    requestMemo: `동시성 테스트 VU=${__VU}`,
  });

  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: { 'Content-Type': 'application/json', 'Authorization': token },
  });

  if (res.status === 201) {
    successCount.add(1);
  } else if (res.status === 409) {
    conflictCount.add(1);
  } else {
    errorRate.add(1);
    console.error(`VU=${__VU} 예상 외 응답: status=${res.status} body=${res.body?.slice(0, 200)}`);
  }

  check(res, {
    '201 또는 409만 반환': (r) => r.status === 201 || r.status === 409,
  });
}

export function handleSummary(data) {
  const checks_total = (data.metrics.checks?.values.passes || 0) + (data.metrics.checks?.values.fails || 0);
  const checks_succeeded = data.metrics.checks?.values.passes || 0;
  const checks_failed = data.metrics.checks?.values.fails || 0;
  const succ_rate = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : "0.00";
  const fail_rate = checks_total > 0 ? ((checks_failed / checks_total) * 100).toFixed(2) : "0.00";
  
  const req_dur = data.metrics.http_req_duration?.values || {};
  const reqs = data.metrics.http_reqs?.values || {};
  const vus = data.metrics.vus?.values || {};
  const iters = data.metrics.iterations?.values || {};
  const iter_dur = data.metrics.iteration_duration?.values || {};
  const data_recv = data.metrics.data_received?.values || {};
  const data_sent = data.metrics.data_sent?.values || {};
  
  const created  = data.metrics.reservation_created?.values?.count  ?? 0;
  const conflict = data.metrics.reservation_conflict?.values?.count ?? 0;
  const errorRate= data.metrics.unexpected_error_rate?.values?.rate ?? 0;
  
  const durationS = (data.state?.testRunDurationMs || 15000) / 1000;
  const formatMs = (val) => val === undefined ? "0.00ms" : val.toFixed(2) + "ms";
  
  let lockMsg = "";
  if (created > 1) {
    lockMsg = `⚠ BEFORE 상태: ${created}건 중복 예약 발생 → Redis 분산 락 적용 필요`;
  } else if (created === 1) {
    lockMsg = `✓ AFTER 상태: 1건만 성공, 분산 락 정상 동작`;
  }
  
  const result = `
[AFTER] 동시 예약 분산 락 테스트  STYLIST_ID=${STYLIST_ID}  SERVICE_ID=${SERVICE_ID}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 201 성공 | 409 중복거부 (정상)

CUSTOM METRICS
  reservation_created     ${created}
  reservation_conflict    ${conflict}
  unexpected_error_rate   ${(errorRate * 100).toFixed(2)}%

  ${lockMsg}

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
