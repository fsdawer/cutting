/**
 * 병목 2: GET /api/stylists/{id} — 상세 조회 (현재 3번 SELECT)
 *
 * Before: profile + services + workingHours = 3 쿼리/요청
 * After:  단일 JPQL JOIN FETCH → 1 쿼리/요청
 *
 * 실행:
 *   cd /Users/jang/Desktop/Study/beauty
 *   k6 run k6/test_stylist_detail.js
 *
 *   # 스타일리스트 ID를 직접 지정하려면:
 *   k6 run -e STYLIST_IDS=1,2,3 k6/test_stylist_detail.js
 *
 *   # 서버 주소 변경:
 *   k6 run -e BASE_URL=http://localhost:8080 k6/test_stylist_detail.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL    = __ENV.BASE_URL || 'http://localhost:8080';
const ID_ENV      = __ENV.STYLIST_IDS || '1,2,3';
const STYLIST_IDS = ID_ENV.split(',').map(Number);

const detailDuration = new Trend('stylist_detail_duration');
const errorRate      = new Rate('stylist_detail_error_rate');

export const options = {
  // k6가 기본으로 수집하는 Trend 통계: avg, min, med, max, p(90), p(95)
  // p(99) 등 추가로 보고 싶으면 아래 주석 해제
  // summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  scenarios: {
    // 1단계: 워밍업 (JVM JIT 안정화)
    warmup: {
      executor: 'constant-arrival-rate',
      rate: 5,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 10,
      tags: { phase: 'warmup' },
    },
    // 2단계: 부하 테스트 (최대 100 RPS까지 램프업)
    load_test: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      stages: [
        { duration: '15s', target: 50  },  // 램프업
        { duration: '30s', target: 100 },  // 풀 부하 유지
        { duration: '10s', target: 0   },  // 쿨다운
      ],
      preAllocatedVUs: 80,
      maxVUs: 150,
      startTime: '12s',
      tags: { phase: 'load' },
    },
  },

  thresholds: {
    // 목표 지연 시간 (Before 기준)
    // 최적화(After) 후에는 p95 < 200ms 로 낮출 것
    'stylist_detail_duration{phase:load}': ['p(95)<800'],
    'stylist_detail_error_rate':           ['rate<0.01'],
    'http_req_failed':                     ['rate<0.01'],
  },
};

export default function () {
  const id  = STYLIST_IDS[__VU % STYLIST_IDS.length]; // VU 번호 기반으로 ID 분산
  const res = http.get(`${BASE_URL}/api/stylists/${id}`, {
    tags: { phase: __ITER < 50 ? 'warmup' : 'load' },
  });

  let body = null;
  try { body = JSON.parse(res.body); } catch (_) {}

  const ok = check(res, {
    'status 200':          (r) => r.status === 200,
    'has services array':  (_) => Array.isArray(body?.services),
    'has workingHours':    (_) => Array.isArray(body?.workingHours),
  });

  if (!ok) {
    console.error(`FAIL id=${id} status=${res.status} body=${res.body?.slice(0, 200)}`);
  }

  detailDuration.add(res.timings.duration);
  errorRate.add(!ok);

  sleep(0.05);
}

export function handleSummary(data) {
  // k6 기본 Trend 키: avg, min, med(=중앙값), max, p(90), p(95)
  const m = data.metrics['stylist_detail_duration']?.values;

  console.log('\n=== 상세 조회 부하테스트 결과 ===');
  if (m) {
    console.log(`avg  : ${m.avg?.toFixed(1)}ms`);
    console.log(`med  : ${m.med?.toFixed(1)}ms`);
    console.log(`p(90): ${m['p(90)']?.toFixed(1)}ms`);
    console.log(`p(95): ${m['p(95)']?.toFixed(1)}ms`);
    console.log(`max  : ${m.max?.toFixed(1)}ms`);
  } else {
    console.log('측정값 없음 — 요청이 모두 실패했을 수 있습니다.');
  }

  const errRate = data.metrics['stylist_detail_error_rate']?.values?.rate;
  console.log(`에러율: ${((errRate ?? 0) * 100).toFixed(2)}%`);

  return {
    'k6/result_stylist_detail.json': JSON.stringify(data, null, 2),
  };
}
