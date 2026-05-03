/**
 * 병목 3: POST /api/reservations — 동시 예약 레이스 컨디션
 *
 * Before: Redis 락 없음 → 동시 요청이 같은 시간대에 중복 예약 생성 가능
 * After:  Redis SETNX 분산 락 → 1건만 성공, 나머지는 409
 *
 * 실행:
 *   cd /Users/jang/Desktop/Study/beauty
 *   k6 run -e TOKEN="Bearer eyJ..." -e STYLIST_ID=1 -e SERVICE_ID=1 k6/test_distributed_lock.js
 *
 * 결과 해석:
 *   Before: http_req_status_201 > 1 이면 중복 예약 발생 (버그 확인)
 *   After:  http_req_status_201 = 1, http_req_status_409 = 나머지 (정상)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const TOKEN      = __ENV.TOKEN      || '';
const STYLIST_ID = __ENV.STYLIST_ID || '1';
const SERVICE_ID = __ENV.SERVICE_ID || '1';

const successCount  = new Counter('reservation_created');   // 201 카운트
const conflictCount = new Counter('reservation_conflict');  // 409 카운트
const errorRate     = new Rate('unexpected_error_rate');    // 201/409 외 응답

// 내일 오전 10시 — 동일 시간대에 모두 요청
function targetTime() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}T10:00:00`;
}

export const options = {
  scenarios: {
    // 10명이 동시에 같은 시간대 예약 시도 — 레이스 컨디션 재현
    concurrent_booking: {
      executor: 'shared-iterations',
      vus: 10,
      iterations: 10,
      maxDuration: '15s',
    },
  },
  thresholds: {
    // After 기준: 예상치 못한 오류(500 등)는 없어야 함
    unexpected_error_rate: ['rate<0.01'],
  },
};

export default function () {
  if (!TOKEN) {
    console.error('TOKEN 환경변수가 필요합니다: -e TOKEN="Bearer eyJ..."');
    return;
  }

  const payload = JSON.stringify({
    stylistId:   Number(STYLIST_ID),
    serviceId:   Number(SERVICE_ID),
    reservedAt:  targetTime(),
    requestMemo: `동시성 테스트 VU=${__VU}`,
  });

  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': TOKEN,
    },
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
  const created  = data.metrics['reservation_created']?.values?.count  ?? 0;
  const conflict = data.metrics['reservation_conflict']?.values?.count ?? 0;

  console.log('\n=== 동시 예약 테스트 결과 ===');
  console.log(`201 Created (예약 성공): ${created}건`);
  console.log(`409 Conflict (중복 거부): ${conflict}건`);

  if (created > 1) {
    console.log('⚠ Before 상태: 중복 예약 발생! Redis 분산 락 적용 필요');
  } else if (created === 1) {
    console.log('✓ After 상태: 1건만 성공, 분산 락 정상 동작');
  }

  return { 'k6/result_distributed_lock.json': JSON.stringify(data, null, 2) };
}
