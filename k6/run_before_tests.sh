#!/bin/bash
# Before 병목 테스트 자동 실행 스크립트
# 실행: bash k6/run_before_tests.sh
# (프로젝트 루트 /Users/jang/Desktop/Study/beauty 에서 실행)

BASE_URL="http://localhost:8080"
USERNAME="jang_user"
PASSWORD="password123"
EMAIL="jang_loadtest@test.com"
STYLIST_ID="1"
SERVICE_ID="1"
DISTRICT="강남구"

echo "========================================"
echo "  CutIng Before 병목 테스트"
echo "========================================"

# 1. 백엔드 헬스체크
echo ""
echo "[1/4] 백엔드 상태 확인..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/stylists" 2>/dev/null)
if [ "$HTTP_STATUS" != "200" ]; then
  echo "  ❌ 백엔드가 응답하지 않습니다 (status: $HTTP_STATUS)"
  echo "  아래 순서로 백엔드를 먼저 실행해주세요:"
  echo "    1. brew services start redis"
  echo "    2. cd /Users/jang/Desktop/Study/beauty && ./gradlew bootRun"
  exit 1
fi
echo "  ✓ 백엔드 정상 (${BASE_URL})"

# 2. 로그인 → 실패 시 회원가입 후 재로그인
echo ""
echo "[2/4] 토큰 발급 중... (username: $USERNAME)"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "  ℹ 로그인 실패 — 테스트 계정 신규 등록 시도 중..."
  curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"email\":\"$EMAIL\",\"name\":\"장민준\",\"phone\":\"010-1234-5678\",\"role\":\"USER\"}" \
    > /dev/null

  LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
  TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
  echo "  ❌ 토큰 발급 실패. 서버 응답:"
  echo "  $LOGIN_RESPONSE"
  exit 1
fi
echo "  ✓ 토큰 발급 완료"
echo "  Bearer ${TOKEN:0:40}..."

# 3. 랭킹 조회 테스트 (인증 불필요)
echo ""
echo "[3/4] 랭킹 조회 병목 테스트 시작 (district: $DISTRICT)"
echo "  → k6 결과: k6/result_ranking_before.json"
echo ""
k6 run -e BASE_URL="$BASE_URL" -e DISTRICT="$DISTRICT" k6/test_ranking_before.js

# 4. 예약 생성 동기 처리 테스트
echo ""
echo "[4/4] 예약 생성 동기 처리 병목 테스트 시작"
echo "  → k6 결과: k6/result_reservation_sync_before.json"
echo ""
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e TOKEN="Bearer $TOKEN" \
  -e STYLIST_ID="$STYLIST_ID" \
  -e SERVICE_ID="$SERVICE_ID" \
  k6/test_reservation_sync_before.js

echo ""
echo "========================================"
echo "  테스트 완료! 결과 파일:"
echo "  - k6/result_ranking_before.json"
echo "  - k6/result_reservation_sync_before.json"
echo ""
echo "  After 테스트 전에 DB 정리:"
echo "  DELETE FROM reservations WHERE request_memo LIKE '%sync_test%';"
echo "========================================"
