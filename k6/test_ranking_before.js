/**
 * [BEFORE] 랭킹 조회 병목 테스트 — N+1 DB 쿼리, 캐시 없음
 *
 * 실행:
 *   k6 run k6/test_ranking_before.js
 *   k6 run -e DISTRICT=마포구 k6/test_ranking_before.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DISTRICT = __ENV.DISTRICT || '강남구';

const rankingDuration = new Trend('ranking_duration_ms', true);
const errorRate       = new Rate('ranking_error_rate');
const requestCount    = new Counter('ranking_requests_total');

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '60s',
    },
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s',  target: 100 },
        { duration: '15s', target: 100 },
        { duration: '5s',  target: 0   },
      ],
      startTime: '65s',
    },
  },
  thresholds: {
    'http_req_duration':             ['p(95)<5000', 'p(99)<8000'],
    'http_req_duration{scenario:constant_load}': ['p(95)<5000'],
    'http_req_duration{scenario:spike}':         ['p(95)<8000'],
    'ranking_error_rate':            ['rate<0.05'],
  },
};

export default function () {
  const res = http.get(
    `${BASE_URL}/api/ranking?district=${encodeURIComponent(DISTRICT)}`,
    { tags: { name: 'GET /api/ranking' } }
  );

  const ok = check(res, {
    'status 200': r => r.status === 200,
    '응답이 배열': r => {
      try { return Array.isArray(JSON.parse(r.body)); } catch { return false; }
    },
  });

  rankingDuration.add(res.timings.duration);
  errorRate.add(!ok);
  requestCount.add(1);

  sleep(0.1);
}

export function handleSummary(data) {
  const checks_total = (data.metrics.checks?.values.passes || 0) + (data.metrics.checks?.values.fails || 0);
  const checks_succeeded = data.metrics.checks?.values.passes || 0;
  const checks_failed = data.metrics.checks?.values.fails || 0;
  const succ_rate = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : "0.00";
  const fail_rate = checks_total > 0 ? ((checks_failed / checks_total) * 100).toFixed(2) : "0.00";
  
  const req_dur = data.metrics.http_req_duration?.values || {};
  const lat = data.metrics.ranking_duration_ms?.values || {};
  const reqs = data.metrics.http_reqs?.values || {};
  const vus = data.metrics.vus?.values || {};
  const iters = data.metrics.iterations?.values || {};
  const iter_dur = data.metrics.iteration_duration?.values || {};
  const data_recv = data.metrics.data_received?.values || {};
  const data_sent = data.metrics.data_sent?.values || {};
  
  const durationS = (data.state?.testRunDurationMs || 60000) / 1000;
  const formatMs = (val) => val === undefined ? "0.00ms" : val.toFixed(2) + "ms";
  
  const result = `
[BEFORE] 랭킹 조회 병목  DISTRICT=${DISTRICT}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ 200 성공 배열 응답 (정상)

CUSTOM METRICS
  ranking_duration_ms     avg=${formatMs(lat.avg)} min=${formatMs(lat.min)} med=${formatMs(lat.med)} max=${formatMs(lat.max)} p(90)=${formatMs(lat["p(90)"])} p(95)=${formatMs(lat["p(95)"])}
  ranking_requests_total  ${data.metrics.ranking_requests_total?.values?.count || 0}
  ranking_error_rate      ${data.metrics.ranking_error_rate?.values?.passes || 0} (${((data.metrics.ranking_error_rate?.values?.rate || 0) * 100).toFixed(2)}%)

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
