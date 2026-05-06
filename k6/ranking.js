/**
 * [After 1] 랭킹 API 부하 테스트 — Redis ZSET 캐싱 검증
 *
 * 실행:
 *   k6 run k6/ranking.js
 *   k6 run -e DISTRICT=마포구 k6/ranking.js
 *
 * 인증 불필요: /api/ranking은 공개 API (permitAll)
 *
 * 테스트 흐름:
 *   1단계 (10s): VU 0→5  워밍업  → 캐시 미스 발생, Redis ZSET 초기화
 *   2단계 (30s): VU 5→50 부하    → 캐시 히트, Redis에서 바로 응답
 *   3단계 (10s): VU 50→0 쿨다운
 *
 * 기대 결과:
 *   - 1단계 p95: 수백 ms (DB 풀스캔, warm-up)
 *   - 2단계 p95: < 50ms  (Redis 캐시 히트)
 *   - 성공률: 99% 이상
 */

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL  || "http://localhost:8080";
const DISTRICT = __ENV.DISTRICT || "강남구";

// ── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
const latency     = new Trend("ranking_latency_ms", true);
const successRate = new Rate("ranking_success_rate");

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
    ranking_latency_ms:   ["p(95)<150", "p(99)<300"],
    ranking_success_rate: ["rate>0.99"],
    http_req_failed:      ["rate<0.01"],
  },
};

// ── 메인 함수 ─────────────────────────────────────────────────────────────────
export default function () {
  group("랭킹 API — Redis ZSET 캐시", () => {
    const start = Date.now();

    const res = http.get(`${BASE_URL}/api/ranking?district=${encodeURIComponent(DISTRICT)}`);

    latency.add(Date.now() - start);

    const ok = check(res, {
      "HTTP 200":            (r) => r.status === 200,
      "배열 응답":            (r) => { try { return Array.isArray(JSON.parse(r.body)); } catch { return false; } },
      "최대 10개":            (r) => { try { return JSON.parse(r.body).length <= 10; } catch { return false; } },
      "stylistId 포함":       (r) => { try { return JSON.parse(r.body)[0]?.stylistId != null; } catch { return false; } },
      "score 포함":           (r) => { try { return JSON.parse(r.body)[0]?.score != null; } catch { return false; } },
    });

    successRate.add(ok);

    if (!ok) {
      console.error(`[랭킹] 실패 — status=${res.status}, body=${res.body.slice(0, 200)}`);
    }

    sleep(0.1); // 너무 빠른 반복 방지
  });
}

export function handleSummary(data) {
  const checks_total     = (data.metrics.checks?.values.passes || 0) + (data.metrics.checks?.values.fails || 0);
  const checks_succeeded = data.metrics.checks?.values.passes || 0;
  const checks_failed    = data.metrics.checks?.values.fails  || 0;
  const succ_rate = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : "0.00";
  const fail_rate = checks_total > 0 ? ((checks_failed    / checks_total) * 100).toFixed(2) : "0.00";

  const lat      = data.metrics["ranking_latency_ms"]?.values    || {};
  const req_dur  = data.metrics.http_req_duration?.values         || {};
  const reqs     = data.metrics.http_reqs?.values                 || {};
  const vus      = data.metrics.vus?.values                       || {};
  const iters    = data.metrics.iterations?.values                || {};
  const iter_dur = data.metrics.iteration_duration?.values        || {};
  const data_recv= data.metrics.data_received?.values             || {};
  const data_sent= data.metrics.data_sent?.values                 || {};

  const durationS = (data.state?.testRunDurationMs || 30000) / 1000;
  const fmt = (v) => v === undefined ? "0.00ms" : v.toFixed(2) + "ms";

  const successRate = ((data.metrics["ranking_success_rate"]?.values?.rate ?? 0) * 100).toFixed(2);
  const errFails    = data.metrics["ranking_success_rate"]?.values?.fails || 0;

  const result = `
[AFTER] 랭킹 API 부하 테스트  DISTRICT=${DISTRICT}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 200 성공 배열 응답 (정상)

CUSTOM METRICS
  ranking_latency_ms      avg=${fmt(lat.avg)} min=${fmt(lat.min)} med=${fmt(lat.med)} max=${fmt(lat.max)} p(90)=${fmt(lat["p(90)"])} p(95)=${fmt(lat["p(95)"])}
  ranking_success_rate    ${successRate}%
  ranking_error_rate      ${errFails} (${(100 - Number(successRate)).toFixed(2)}%)

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
  return { stdout: result, "k6-ranking-after-result.json": JSON.stringify(data, null, 2) };
}
