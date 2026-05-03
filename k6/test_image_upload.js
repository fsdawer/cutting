/**
 * 병목 4: POST /api/reservations/{id}/images — 이미지 업로드 (동기 파일 I/O)
 *
 * 현재 문제: Files.copy() 동기 블로킹 → Tomcat 스레드 소진 위험
 *   - 스레드 1개가 파일 쓰기 완료될 때까지 block
 *   - 동시 업로드 요청이 몰리면 다른 API(예약/결제)까지 지연
 *
 * Before: 동기 Files.copy() — 스레드 block
 * After:  S3 비동기 업로드 or @Async + CompletableFuture
 *
 * 전제조건:
 *   1. DB에 reservation이 존재해야 함 (RESERVATION_IDS에 실제 ID 입력)
 *   2. 업로드 테스트용 더미 이미지 파일이 k6/fixtures/test.jpg 에 있어야 함
 *      없으면 스크립트가 base64 인코딩 더미 데이터를 직접 생성함
 *
 * 실행 방법:
 *   k6 run k6/test_image_upload.js -e TOKEN=Bearer\ your-jwt-token
 *
 * 환경변수:
 *   BASE_URL  - 백엔드 주소 (기본: http://localhost:8080)
 *   TOKEN     - JWT 인증 토큰 (기본: 없음 → 401)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const uploadDuration = new Trend('image_upload_duration', true);
const errorRate      = new Rate('image_upload_error_rate');
const threadTimeout  = new Rate('thread_timeout_rate');  // 서버 스레드 고갈 지표

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN    = __ENV.TOKEN    || '';

// 실제 DB에 있는 예약 ID로 교체 (본인 예약 ID여야 403 안 남)
const RESERVATION_IDS = [1, 2, 3, 4, 5];

// 더미 JPEG 데이터 (1x1 픽셀 최소 JPEG)
const DUMMY_JPEG_B64 =
  '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8U' +
  'HRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgN' +
  'DRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy' +
  'MjL/wAARCAABAAEDASIAAhEBAxEB/8QAFgABAQEAAAAAAAAAAAAAAAAABgUEB' +
  '/8QAIBAAAgIBBQEBAAAAAAAAAAAAAQIDBAUREiExQf/EABUBAQEAAAAAAAAAAAAAAAAAAAAB' +
  '/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AltJagyeH0AthI5xdrLcNM91' +
  'BF5pX2HaH9bcfaSXWGaRmknyJckliyjqTzSlT54b6bk+h0R//2Q==';

function makeDummyJpeg() {
  return http.file(
    new Uint8Array(atob(DUMMY_JPEG_B64).split('').map(c => c.charCodeAt(0))),
    'test.jpg',
    'image/jpeg'
  );
}

export const options = {
  scenarios: {
    // 1단계: 낮은 동시성 (스레드 여유 있는 상태)
    low_concurrency: {
      executor: 'constant-vus',
      vus: 5,
      duration: '20s',
      tags: { phase: 'low' },
    },
    // 2단계: 높은 동시성 (스레드 소진 압박)
    high_concurrency: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      startTime: '22s',
      tags: { phase: 'high' },
    },
  },
  thresholds: {
    // Before 목표: p95 < 3000ms (동기 I/O라 느림)
    // After  목표: p95 < 500ms  (비동기 처리 후)
    'image_upload_duration{phase:low}':  ['p(95)<2000'],
    'image_upload_duration{phase:high}': ['p(95)<5000'],  // 스레드 경합 감안
    'image_upload_error_rate':           ['rate<0.05'],
    // 30초 타임아웃 비율 — 높으면 스레드 고갈
    'thread_timeout_rate':               ['rate<0.02'],
  },
};

export default function () {
  if (!TOKEN) {
    console.warn('TOKEN 환경변수가 없습니다. -e TOKEN="Bearer xxx" 로 실행하세요.');
    return;
  }

  const reservationId = RESERVATION_IDS[Math.floor(Math.random() * RESERVATION_IDS.length)];
  const url           = `${BASE_URL}/api/reservations/${reservationId}/images`;

  const formData = {
    images: makeDummyJpeg(),
  };

  const start = Date.now();
  const res = http.post(url, formData, {
    headers: { Authorization: TOKEN },
    timeout: '30s',
    tags: { endpoint: 'image_upload' },
  });
  const elapsed = Date.now() - start;

  const timedOut = res.status === 0 || elapsed >= 29000;
  threadTimeout.add(timedOut);

  const ok = check(res, {
    'status 200':       (r) => r.status === 200,
    'not timed out':    (_) => !timedOut,
    'latency < 2000ms': (r) => r.timings.duration < 2000,
  });

  uploadDuration.add(res.timings.duration);
  errorRate.add(!ok);

  // 업로드 사이 간격 — 실제 유저는 이미지 선택하는 시간이 있음
  sleep(Math.random() * 2 + 0.5);
}

export function handleSummary(data) {
  const low  = data.metrics['image_upload_duration']?.values;
  const high = data.metrics['image_upload_duration']?.values;

  console.log('=== 이미지 업로드 부하테스트 결과 ===');
  if (low) {
    console.log(`전체 p50: ${low['p(50)']?.toFixed(1)}ms`);
    console.log(`전체 p95: ${low['p(95)']?.toFixed(1)}ms`);
    console.log(`전체 p99: ${low['p(99)']?.toFixed(1)}ms`);
  }
  console.log('스레드 타임아웃 비율:', data.metrics['thread_timeout_rate']?.values?.rate?.toFixed(4));

  return {
    'k6/result_image_upload.json': JSON.stringify(data),
  };
}
