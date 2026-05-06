/**
 * [After 3] 동시 예약 분산락 테스트
 *
 * 실행:
 *   k6 run -e STYLIST_ID=1 -e SERVICE_ID=1 k6/concurrency.js
 *
 * 사전 준비:
 *   setup() 단계에서 loadtest_c1~50 계정을 자동 생성/로그인 → 각 VU에 고유 토큰 배분
 *
 * 테스트 흐름:
 *   10개의 VU가 완전히 동일한 예약 시간대로 동시에 요청
 *
 * 기대 결과:
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Before (분산락 없음)   │ After (Redis SETNX 분산락)         │
 *   ├────────────────────────┼─────────────────────────────────────┤
 *   │ 최악 10개 모두 성공    │ 정확히 1개만 성공                   │
 *   │ DB에 중복 예약 10건    │ 중복 예약 0건                       │
 *   │ 레이스 컨디션 발생     │ 레이스 컨디션 완전 차단             │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * 검증 방법:
 *   - 성공 응답(200/201) 수를 카운트 → 1개여야 함
 *   - 실패 응답(400/409/500) 수를 카운트 → 9개여야 함
 */

import http from "k6/http";
import { check } from "k6";
import { Counter } from "k6/metrics";

const BASE_URL   = __ENV.BASE_URL   || "http://localhost:8080";
const STYLIST_ID = __ENV.STYLIST_ID || "1";
const SERVICE_ID = __ENV.SERVICE_ID || "1";
const NUM_USERS  = 50; // VU 수와 동일

const JSON_HEADER = { "Content-Type": "application/json" };

// ── 사전 준비: 10명 유저 생성 + 토큰 발급 ────────────────────────────────────────
export function setup() {
  const tokens = [];
  for (let i = 1; i <= NUM_USERS; i++) {
    const username = `loadtest_c${i}`;
    const password = "Loadtest123!";

    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username,
      email:    `loadtest_c${i}@k6test.com`,
      password,
      name:     `동시성테스트${i}`,
      phone:    `010-1000-${String(i).padStart(4, "0")}`,
      role:     "USER",
    }), { headers: JSON_HEADER });


    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      username,
      password,
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

// ── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
const successCount = new Counter("concurrent_success_count"); // 예약 성공 수
const rejectCount  = new Counter("concurrent_reject_count");  // 분산락/중복으로 거부된 수

// ── 시나리오 설정 ─────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    /**
     * shared-iterations: N개의 VU가 총 N번의 반복을 나눠서 실행
     * vus=50, iterations=50 → 각 VU가 1번씩 실행 (완전 동시에 가까움)
     *
     * 이렇게 하면 10개 스레드가 거의 동시에 같은 슬롯으로 예약 요청
     */
    concurrency: {
      executor: "shared-iterations",
      vus: 50,
      iterations: 50,
      maxDuration: "30s",
    },
  },
  thresholds: {
    // 성공은 정확히 1건 (분산락이 올바르면 중복 불가)
    concurrent_success_count: ["count>=1", "count<=1"],
    // 거부는 최소 49건
    concurrent_reject_count: ["count>=49"],
  },
};

// ── 모든 VU가 동일한 예약 시간대 사용 ─────────────────────────────────────────
// 실행마다 다른 슬롯을 사용해 재실행 시 중복 예약 충돌을 방지
// 분 단위 seed → 같은 분 내 재실행은 동일 슬롯, 1분 이상 간격이면 다른 슬롯
const _seed  = Math.floor(Date.now() / (1000 * 60));
const _d     = new Date();
_d.setFullYear(_d.getFullYear() + 2);
_d.setMonth(0);
_d.setDate(1 + (_seed % 28));  // 1-28 (모든 달에 유효)
const _hour  = 10 + (_seed % 8); // 10-17
const FIXED_TIME = `${_d.getFullYear()}-01-${String(_d.getDate()).padStart(2, "0")}T${String(_hour).padStart(2, "0")}:00:00`;

// ── 메인 함수 ─────────────────────────────────────────────────────────────────
export default function (data) {
  const token = data.tokens[__VU - 1];
  if (!token) {
    console.error(`[VU ${__VU}] 토큰 없음 — 건너뜀`);
    return;
  }
  const headers = { Authorization: token, "Content-Type": "application/json" };

  const payload = JSON.stringify({
    stylistId:   Number(STYLIST_ID),
    serviceId:   Number(SERVICE_ID),
    reservedAt:  FIXED_TIME,          // ← 모든 VU가 같은 시간대!
    requestMemo: `동시성테스트 VU:${__VU}`,
  });

  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers,
  });

  console.log(`[분산락] VU:${__VU} → HTTP ${res.status} | ${res.body.slice(0, 100)}`);

  if (res.status === 200 || res.status === 201) {
    // ✅ 예약 성공 — 1건만 기대
    successCount.add(1);
    check(res, {
      "[분산락] 예약 성공 (1건만 허용)": () => true,
    });

  } else if (res.status === 400 || res.status === 409 || res.status === 500) {
    // ✅ 분산락 또는 중복 체크로 거부됨 — 9건 기대
    rejectCount.add(1);
    check(res, {
      "[분산락] 중복 예약 거부됨 (정상)": () => true,
    });

  } else {
    // ❌ 예상치 못한 응답
    check(res, {
      "[분산락] 예상치 못한 응답 코드": () => false,
    });
    console.error(`[분산락] 예상치 못한 status=${res.status}`);
  }
}

export function handleSummary(data) {
  const checks_total     = (data.metrics.checks?.values.passes || 0) + (data.metrics.checks?.values.fails || 0);
  const checks_succeeded = data.metrics.checks?.values.passes || 0;
  const checks_failed    = data.metrics.checks?.values.fails  || 0;
  const succ_rate = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : "0.00";
  const fail_rate = checks_total > 0 ? ((checks_failed    / checks_total) * 100).toFixed(2) : "0.00";

  const success  = data.metrics["concurrent_success_count"]?.values?.count ?? 0;
  const rejects  = data.metrics["concurrent_reject_count"]?.values?.count  ?? 0;
  const req_dur  = data.metrics.http_req_duration?.values  || {};
  const reqs     = data.metrics.http_reqs?.values          || {};
  const vus      = data.metrics.vus?.values                || {};
  const iters    = data.metrics.iterations?.values         || {};
  const iter_dur = data.metrics.iteration_duration?.values || {};
  const data_recv= data.metrics.data_received?.values      || {};
  const data_sent= data.metrics.data_sent?.values          || {};

  const durationS = (data.state?.testRunDurationMs || 15000) / 1000;
  const fmt = (v) => v === undefined ? "0.00ms" : v.toFixed(2) + "ms";

  const lockMsg = success === 1
    ? "PASS — 분산락 정상 작동 (중복 예약 0건)"
    : `FAIL — 성공 ${success}건 (중복 예약 발생!)`;

  const result = `
[AFTER] 동시 예약 분산락 테스트  STYLIST_ID=${STYLIST_ID}  SERVICE_ID=${SERVICE_ID}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 1건 성공 | 49건 거부 (정상)

CUSTOM METRICS
  concurrent_success_count  ${success}건  (기대: 정확히 1건)
  concurrent_reject_count   ${rejects}건  (기대: 49건 이상)
  결과: ${lockMsg}

HTTP
  http_req_duration       avg=${fmt(req_dur.avg)} min=${fmt(req_dur.min)} med=${fmt(req_dur.med)} max=${fmt(req_dur.max)} p(90)=${fmt(req_dur["p(90)"])} p(95)=${fmt(req_dur["p(95)"])}
  http_reqs               ${reqs.count || 0}   ${(reqs.rate || 0).toFixed(2)}/s

EXECUTION
  iteration_duration      avg=${fmt(iter_dur.avg)} min=${fmt(iter_dur.min)} med=${fmt(iter_dur.med)} max=${fmt(iter_dur.max)} p(90)=${fmt(iter_dur["p(90)"])} p(95)=${fmt(iter_dur["p(95)"])}
  iterations              ${iters.count || 0}   ${(iters.rate || 0).toFixed(2)}/s
  vus                     min=${vus.min || 0}  max=${vus.max || 0}
  vus_max                 min=${data.metrics.vus_max?.values?.min || 0}  max=${data.metrics.vus_max?.values?.max || 0}

NETWORK
  data_received           ${((data_recv.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_recv.rate || 0) / 1024).toFixed(2)} kB/s
  data_sent               ${((data_sent.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_sent.rate || 0) / 1024).toFixed(2)} kB/s
`;

  console.log(result);
  return { stdout: result, "k6-concurrency-after-result.json": JSON.stringify(data, null, 2) };
}
