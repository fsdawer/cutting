/**
 * 병목 2: GET /api/stylists + GET /api/stylists/{id} — 캐시 적용 전후
 *
 * Before: 요청마다 DB SELECT 5회 (findById + lazy User + lazy Salon + services + hours)
 * After:  Redis 캐시 히트 → DB 0회, 응답 1~2ms
 *
 * 실행:
 *   cd /Users/jang/Desktop/Study/beauty
 *   k6 run k6/test_stylist_cache.js
 *   k6 run -e STYLIST_IDS=1,2,3 k6/test_stylist_cache.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8080';
const ID_ENV      = __ENV.STYLIST_IDS || '1,2,3';
const STYLIST_IDS = ID_ENV.split(',').map(Number);

const listDuration   = new Trend('stylist_list_duration');
const detailDuration = new Trend('stylist_detail_duration');
const errorRate      = new Rate('error_rate');

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-arrival-rate',
      rate: 5,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 10,
      tags: { phase: 'warmup' },
    },
    load: {
      executor: 'ramping-arrival-rate',
      startRate: 20,
      timeUnit: '1s',
      stages: [
        { duration: '15s', target: 60  },
        { duration: '30s', target: 100 },
        { duration: '10s', target: 0   },
      ],
      preAllocatedVUs: 80,
      maxVUs: 150,
      startTime: '12s',
      tags: { phase: 'load' },
    },
  },
  thresholds: {
    // Before 목표 (캐시 없음): p95 < 800ms
    // After  목표 (캐시 적용): p95 < 50ms
    'stylist_list_duration{phase:load}':   ['p(95)<800'],
    'stylist_detail_duration{phase:load}': ['p(95)<800'],
    error_rate: ['rate<0.01'],
  },
};

export default function () {
  group('목록 조회 GET /api/stylists', () => {
    const res = http.get(`${BASE_URL}/api/stylists`);
    const ok  = check(res, { 'status 200': (r) => r.status === 200 });
    listDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  const id  = STYLIST_IDS[__VU % STYLIST_IDS.length];

  group(`상세 조회 GET /api/stylists/${id}`, () => {
    const res = http.get(`${BASE_URL}/api/stylists/${id}`);
    const ok  = check(res, {
      'status 200':       (r) => r.status === 200,
      'has workingHours': (r) => {
        try { return Array.isArray(JSON.parse(r.body).workingHours); } catch { return false; }
      },
    });
    detailDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.05);
}

export function handleSummary(data) {
  const list   = data.metrics['stylist_list_duration']?.values;
  const detail = data.metrics['stylist_detail_duration']?.values;

  console.log('\n=== 스타일리스트 캐시 부하테스트 결과 ===');
  if (list) {
    console.log('[목록 조회]');
    console.log(`  avg: ${list.avg?.toFixed(1)}ms | med: ${list.med?.toFixed(1)}ms | p(95): ${list['p(95)']?.toFixed(1)}ms`);
  }
  if (detail) {
    console.log('[상세 조회]');
    console.log(`  avg: ${detail.avg?.toFixed(1)}ms | med: ${detail.med?.toFixed(1)}ms | p(95): ${detail['p(95)']?.toFixed(1)}ms`);
  }
  console.log('\nBefore → After 비교:');
  console.log('  목록 p(95): DB 조회 시 ~200ms → 캐시 적용 후 ~5ms 목표');
  console.log('  상세 p(95): DB 5회 조회 시 ~50ms → 캐시 적용 후 ~2ms 목표');

  return { 'k6/result_stylist_cache.json': JSON.stringify(data, null, 2) };
}
