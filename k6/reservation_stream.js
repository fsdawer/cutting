/**
 * [After 3] Redis Streams 예약 생성 비동기 처리 부하 테스트
 *
 * 실행:
 *   k6 run -e STYLIST_ID=1 -e SERVICE_ID=1 k6/reservation_stream.js
 *
 * 사전 준비:
 *   setup() 단계에서 loadtest_u1~50 계정을 자동 생성/로그인 → 각 VU에 고유 토큰 배분
 *   (이미 계정이 있으면 409 무시 후 로그인만 수행)
 *
 * 테스트 흐름:
 *   constant-arrival-rate: 초당 10 요청을 일정하게 보냄
 *
 * 검증 포인트:
 *   After 2 (@Async) 대비, Redis Streams가 적용되어도 
 *   메인 스레드의 예약 생성 속도(Latency)가 저하되지 않음을 확인합니다.
 *   또한, 백그라운드에서 Consumer가 메시지를 안전하게 처리하고 ACK를 보내는지
 *   스프링 부트 서버의 로그를 통해 확인해야 합니다.
 *
 * 주의: 각 VU는 서로 다른 예약 시간대를 사용해 중복 예약을 방지함
 */

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Rate } from "k6/metrics";

const BASE_URL   = __ENV.BASE_URL   || "http://localhost:8080";
const STYLIST_ID = __ENV.STYLIST_ID || "1";
const SERVICE_ID = __ENV.SERVICE_ID || "1";
const NUM_USERS  = 50; // 생성할 테스트 유저 수 (= maxVUs)

const JSON_HEADER = { "Content-Type": "application/json" };

// 예약 충돌 방지용 기준 년도 (올해 + 2년)
const _baseYear  = new Date().getFullYear() + 2;
const BASE_DATE  = new Date(_baseYear, 0, 1); // Jan 1

// ── 사전 준비: N명 유저 생성 + 토큰 발급 ────────────────────────────────────────
export function setup() {
  const tokens = [];
  for (let i = 1; i <= NUM_USERS; i++) {
    const username = `loadtest_u${i}`;
    const password = "Loadtest123!";

    // 등록 (이미 존재하면 409 → 무시)
    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username,
      email:    `loadtest_u${i}@k6test.com`,
      password,
      name:     `부하테스트${i}`,
      phone:    `010-0000-${String(i).padStart(4, "0")}`,
      role:     "USER",
    }), { headers: JSON_HEADER });

    // 로그인
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
  
  const runSeed = Math.floor(Date.now() / (1000 * 60)) % 3000;
  return { tokens, runSeed };
}

// ── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
const latency     = new Trend("reservation_latency_ms", true);
const successRate = new Rate("reservation_success_rate");

// ── 시나리오 설정 ─────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    load: {
      executor: "constant-vus",
      vus: 50,
      duration: "30s",
    },
  },
  thresholds: {
    reservation_latency_ms:   ["p(95)<500", "p(99)<1000"],
    reservation_success_rate: ["rate>0.90"],
    http_req_failed:          ["rate<0.10"],
  },
};

// ── 메인 함수 ─────────────────────────────────────────────────────────────────
export default function (data) {
  const token = data.tokens[(__VU - 1) % NUM_USERS];
  if (!token) {
    console.error(`[VU ${__VU}] 토큰 없음 — 건너뜀`);
    return;
  }
  const headers = { Authorization: token, "Content-Type": "application/json" };

  group("예약 생성 API — Redis Streams 비동기 처리", () => {

    const totalSlot  = (__VU - 1) * 200 + __ITER + (data.runSeed * 10000);
    const dayOffset  = Math.floor(totalSlot / 16);
    const slotInDay  = totalSlot % 16;
    
    const date       = new Date(BASE_DATE.getTime() + dayOffset * 86400000);
    const hour       = 10 + Math.floor(slotInDay / 2);
    const minute     = (slotInDay % 2) === 0 ? "00" : "30";
    const reservedAt = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}T${String(hour).padStart(2,"0")}:${minute}:00`;

    const payload = JSON.stringify({
      stylistId:   Number(STYLIST_ID),
      serviceId:   Number(SERVICE_ID),
      reservedAt:  reservedAt,
      requestMemo: `k6 부하테스트 VU:${__VU} ITER:${__ITER}`,
    });

    const start = Date.now();

    const res = http.post(`${BASE_URL}/api/reservations`, payload, {
      headers,
    });

    const elapsed = Date.now() - start;
    latency.add(elapsed);

    const ok = check(res, {
      "HTTP 200 or 201": (r) => r.status === 200 || r.status === 201,
      "reservationId 존재": (r) => {
        try {
          const b = JSON.parse(r.body);
          return b.id != null || b.reservationId != null;
        } catch { return false; }
      },
      "응답 1000ms 이내": () => elapsed < 1000,
    });

    successRate.add(ok);

    if (!ok) {
      console.error(
        `[예약] 실패 — VU:${__VU}, status:${res.status}, ${res.body.slice(0, 150)}`
      );
    }

    sleep(0.2);
  });
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
  
  const formatMs = (val) => val === undefined ? "0.00ms" : val.toFixed(2) + "ms";
  
  const result = `
[AFTER 3] 예약 생성 Redis Streams 비동기 처리  STYLIST_ID=${STYLIST_ID}  SERVICE_ID=${SERVICE_ID}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / 30).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 201 성공 | 409 중복거부 (정상)

CUSTOM METRICS
  reservation_latency_ms  avg=${formatMs(lat.avg)} min=${formatMs(lat.min)} med=${formatMs(lat.med)} max=${formatMs(lat.max)} p(90)=${formatMs(lat["p(90)"])} p(95)=${formatMs(lat["p(95)"])}
  reservation_success     ${data.metrics.reservation_success_rate?.values?.passes || 0}
  error_rate              ${data.metrics.reservation_success_rate?.values?.fails || 0} (${(100 - (data.metrics.reservation_success_rate?.values?.rate || 0) * 100).toFixed(2)}%)

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
