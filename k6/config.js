/**
 * k6 부하 테스트 — 시나리오별 단독 실행 버전
 *
 * 사용법:
 *   k6 run k6/ranking.js          # After 1: 랭킹 캐시 테스트
 *   k6 run k6/reservation.js      # After 2: 비동기 예약 테스트
 *   k6 run k6/concurrency.js      # After 3: 분산락 동시성 테스트
 *   k6 run k6-test.js             # 전체 순차 실행
 *
 * 환경 변수 설정 예시:
 *   k6 run -e JWT_TOKEN=eyJ... -e STYLIST_ID=1 -e SERVICE_ID=1 k6/ranking.js
 */

// ── 공통 설정 (각 파일에서 import해서 사용) ──────────────────────────────────
export const BASE_URL    = __ENV.BASE_URL    || "http://localhost:8080";
export const JWT_TOKEN   = __ENV.JWT_TOKEN   || "여기에-JWT-토큰-입력";
export const STYLIST_ID  = __ENV.STYLIST_ID  || "1";
export const SERVICE_ID  = __ENV.SERVICE_ID  || "1";
export const DISTRICT    = __ENV.DISTRICT    || "강남구";

export const HEADERS = {
  Authorization: `Bearer ${JWT_TOKEN}`,
  "Content-Type": "application/json",
};
