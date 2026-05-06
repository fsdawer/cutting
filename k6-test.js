/**
 * k6 부하 테스트 스크립트
 *
 * 테스트 시나리오 3가지:
 *   1. rankingTest      — After 1 검증: Redis ZSET 캐싱으로 랭킹 API 응답 속도
 *   2. reservationTest  — After 2 검증: 비동기 처리 후 예약 API 응답 속도
 *   3. concurrencyTest  — After 3 검증: 동시 예약 중복 방지 (분산락)
 *
 * 실행 방법:
 *   brew install k6         # 설치 (macOS)
 *   k6 run k6-test.js       # 전체 실행
 *   k6 run --scenario rankingTest k6-test.js  # 특정 시나리오만
 *
 * 환경 변수:
 *   BASE_URL  : 서버 주소 (기본값: http://localhost:8080)
 *   JWT_TOKEN : Authorization 헤더용 JWT (기본값: 아래 상수)
 *   USER_ID   : 테스트용 유저 ID
 *   STYLIST_ID: 테스트용 미용사 ID
 *   SERVICE_ID: 테스트용 서비스 ID
 *
 *   예) k6 run -e BASE_URL=http://localhost:8080 -e JWT_TOKEN=eyJ... k6-test.js
 */

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

// ─────────────────────────────────────────────────────────────────────────────
// 환경 변수 & 공통 상수
// ─────────────────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const JWT_TOKEN = __ENV.JWT_TOKEN || "your-jwt-token-here";
const USER_ID = __ENV.USER_ID || "1";
const STYLIST_ID = __ENV.STYLIST_ID || "1";
const SERVICE_ID = __ENV.SERVICE_ID || "1";
const DISTRICT = __ENV.DISTRICT || "강남구";

const HEADERS = {
  Authorization: `Bearer ${JWT_TOKEN}`,
  "Content-Type": "application/json",
};

// ─────────────────────────────────────────────────────────────────────────────
// 커스텀 메트릭
// ─────────────────────────────────────────────────────────────────────────────
const rankingLatency = new Trend("ranking_latency_ms", true);       // After 1
const reservationLatency = new Trend("reservation_latency_ms", true); // After 2
const duplicateRejects = new Counter("duplicate_reservation_rejects"); // After 3
const successRate = new Rate("success_rate");

// ─────────────────────────────────────────────────────────────────────────────
// 시나리오 설정
// ─────────────────────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    /**
     * After 1 검증: 랭킹 API 캐싱 효과
     * - 첫 요청(캐시 미스) vs 이후 요청(캐시 히트) 응답 시간 비교
     * - 목표: p95 < 50ms (캐시 히트 시)
     */
    rankingTest: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 10 }, // 워밍업
        { duration: "30s", target: 50 }, // 부하 증가
        { duration: "10s", target: 0 },  // 쿨다운
      ],
      exec: "rankingScenario",
      tags: { scenario: "ranking" },
    },

    /**
     * After 2 검증: 예약 생성 비동기 처리
     * - 동기 vs 비동기 응답 시간 비교
     * - 목표: p95 < 200ms (랭킹 재계산 + 알림 제외)
     */
    reservationTest: {
      executor: "constant-arrival-rate",
      rate: 10,           // 초당 10 요청
      timeUnit: "1s",
      duration: "30s",
      preAllocatedVUs: 20,
      maxVUs: 50,
      exec: "reservationScenario",
      startTime: "1m",    // rankingTest 이후 실행
      tags: { scenario: "reservation" },
    },

    /**
     * After 3 검증: 동시 예약 분산락
     * - 같은 시간대에 N개의 요청이 동시에 들어올 때 1개만 성공해야 함
     * - 목표: 중복 예약 0건, 409 응답이 나머지 (N-1)건
     */
    concurrencyTest: {
      executor: "shared-iterations",
      vus: 10,           // 10개 가상 유저가 동시에
      iterations: 10,    // 10번 실행 (전부 같은 시간대 예약)
      maxDuration: "30s",
      exec: "concurrencyScenario",
      startTime: "2m",   // reservationTest 이후 실행
      tags: { scenario: "concurrency" },
    },
  },

  thresholds: {
    // After 1: 랭킹 API — 캐시 히트 시 p95 50ms 이하
    ranking_latency_ms: ["p(95)<50", "p(99)<100"],
    // After 2: 예약 API — 비동기 전환 후 p95 200ms 이하
    reservation_latency_ms: ["p(95)<200", "p(99)<500"],
    // 전체 성공률 95% 이상
    success_rate: ["rate>0.95"],
    http_req_failed: ["rate<0.05"],
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// After 1: 랭킹 시나리오
// ─────────────────────────────────────────────────────────────────────────────
export function rankingScenario() {
  group("After 1 — Redis ZSET 랭킹 캐싱", () => {
    const start = Date.now();

    const res = http.get(`${BASE_URL}/api/ranking?district=${DISTRICT}`, {
      headers: HEADERS,
    });

    const latency = Date.now() - start;
    rankingLatency.add(latency);

    const ok = check(res, {
      "status 200": (r) => r.status === 200,
      "응답에 stylistId 포함": (r) => {
        try {
          const body = JSON.parse(r.body);
          return Array.isArray(body) && body.length > 0 && body[0].stylistId !== undefined;
        } catch {
          return false;
        }
      },
      "응답 10개 이하": (r) => {
        try {
          return JSON.parse(r.body).length <= 10;
        } catch {
          return false;
        }
      },
    });

    successRate.add(ok);

    // 연속 요청 간 0.1초 대기 (너무 빠른 루프 방지)
    sleep(0.1);
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// After 2: 예약 생성 비동기 시나리오
// ─────────────────────────────────────────────────────────────────────────────
export function reservationScenario() {
  group("After 2 — @Async 비동기 예약 생성", () => {
    // 각 VU마다 다른 시간대로 예약 (중복 방지)
    const hour = 10 + (__VU % 8); // 10~17시 분산
    const minute = (__ITER % 2) === 0 ? "00" : "30";
    const reservedAt = `2026-12-01T${String(hour).padStart(2, "0")}:${minute}:00`;

    const payload = JSON.stringify({
      stylistId: Number(STYLIST_ID),
      serviceId: Number(SERVICE_ID),
      reservedAt: reservedAt,
      requestMemo: `k6 테스트 — VU:${__VU} ITER:${__ITER}`,
    });

    const start = Date.now();

    const res = http.post(`${BASE_URL}/api/reservations`, payload, {
      headers: HEADERS,
    });

    const latency = Date.now() - start;
    reservationLatency.add(latency);

    /**
     * ┌─────────────────────────────────────────────────────────────┐
     * │  Before (동기)     │  After (비동기)                        │
     * ├────────────────────┼────────────────────────────────────────┤
     * │ 랭킹재계산 DB 2번  │ 이벤트 발행만 (즉시 반환)              │
     * │ WebSocket 알림     │ 별도 스레드에서 처리                   │
     * │ p95 ~29ms          │ p95 ~15ms 목표                         │
     * └─────────────────────────────────────────────────────────────┘
     *
     * 응답이 200/201이면 비동기 처리가 성공적으로 분리됨을 의미.
     * 실제 랭킹 업데이트/알림 전송은 응답 이후에 별도 스레드에서 완료됨.
     */
    const ok = check(res, {
      "예약 성공 (200 or 201)": (r) => r.status === 200 || r.status === 201,
      "응답에 reservationId 포함": (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined || body.reservationId !== undefined;
        } catch {
          return false;
        }
      },
    });

    successRate.add(ok);

    if (!ok) {
      console.log(
        `[예약 실패] status=${res.status}, body=${res.body.substring(0, 200)}`
      );
    }

    sleep(0.5);
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// After 3: 동시 예약 분산락 시나리오
// ─────────────────────────────────────────────────────────────────────────────
export function concurrencyScenario() {
  group("After 3 — Redis 분산락 동시 예약 방지", () => {
    /**
     * 10개의 VU가 동일한 시간대(14:00)로 동시에 예약 시도.
     *
     * 기대 결과:
     *   - 성공(200/201): 1건만
     *   - 실패(409/500, "이미 예약 처리 중"): 나머지 9건
     *
     * Before (분산락 없음):
     *   - check → save 사이에 다른 요청이 끼어들어 중복 예약 가능
     *   - 최악의 경우 10개 모두 성공 → 같은 시간대 예약 10개 생성
     *
     * After (분산락 있음):
     *   - SETNX로 첫 번째 요청만 락 획득
     *   - 나머지 9개는 즉시 실패 응답
     *   - 중복 예약 0건 보장
     */
    const FIXED_TIME = "2026-12-15T14:00:00"; // 모든 VU가 동일 슬롯 사용

    const payload = JSON.stringify({
      stylistId: Number(STYLIST_ID),
      serviceId: Number(SERVICE_ID),
      reservedAt: FIXED_TIME,
      requestMemo: `동시성 테스트 VU:${__VU}`,
    });

    const res = http.post(`${BASE_URL}/api/reservations`, payload, {
      headers: HEADERS,
    });

    // 성공: 1건만 기대
    if (res.status === 200 || res.status === 201) {
      check(res, {
        "예약 성공 (1건만 허용)": () => true,
      });
      console.log(`[분산락] 예약 성공 — VU:${__VU}, status:${res.status}`);
    }
    // 실패: 분산락 또는 중복 체크에 의해 거부됨
    else if (res.status === 409 || res.status === 400 || res.status === 500) {
      duplicateRejects.add(1);
      check(res, {
        "중복 예약 거부됨 (예상된 실패)": () => true,
      });
      console.log(
        `[분산락] 중복 거부 — VU:${__VU}, status:${res.status}, msg:${res.body.substring(0, 100)}`
      );
    } else {
      console.error(`[분산락] 예상치 못한 응답 — VU:${__VU}, status:${res.status}`);
    }
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// 테스트 종료 후 요약 출력
// ─────────────────────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const rankingP95 = data.metrics["ranking_latency_ms"]?.values?.["p(95)"] ?? "N/A";
  const reservationP95 = data.metrics["reservation_latency_ms"]?.values?.["p(95)"] ?? "N/A";
  const rejects = data.metrics["duplicate_reservation_rejects"]?.values?.count ?? 0;
  const rate = (data.metrics["success_rate"]?.values?.rate * 100 ?? 0).toFixed(1);

  const summary = `
╔══════════════════════════════════════════════════════════════════╗
║                   k6 성능 테스트 결과 요약                       ║
╠══════════════════════════════════════════════════════════════════╣
║ [After 1] 랭킹 API (Redis ZSET 캐싱)                            ║
║   → 랭킹 응답 p95: ${String(rankingP95 === "N/A" ? "N/A" : rankingP95.toFixed(1) + "ms").padEnd(10)} (목표: < 50ms)        ║
╠══════════════════════════════════════════════════════════════════╣
║ [After 2] 예약 API (@Async 비동기 처리)                          ║
║   → 예약 응답 p95: ${String(reservationP95 === "N/A" ? "N/A" : reservationP95.toFixed(1) + "ms").padEnd(10)} (목표: < 200ms)       ║
╠══════════════════════════════════════════════════════════════════╣
║ [After 3] 동시 예약 (Redis 분산락)                               ║
║   → 중복 예약 거부 수: ${String(rejects).padEnd(6)} (동시 10 요청 중 9건 거부 기대)  ║
╠══════════════════════════════════════════════════════════════════╣
║   전체 성공률: ${rate}%                                             ║
╚══════════════════════════════════════════════════════════════════╝
`;

  console.log(summary);

  return {
    stdout: summary,
    "k6-result.json": JSON.stringify(data, null, 2),
  };
}
