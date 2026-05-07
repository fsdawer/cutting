-- =============================================================
-- 인덱스 추가 마이그레이션
-- Hibernate ddl-auto가 create/create-drop이면 @Table(indexes)로 자동 생성.
-- update 모드이거나 운영 DB라면 이 스크립트를 직접 실행.
-- =============================================================

-- ── reservations ──────────────────────────────────────────────
-- 내 예약 목록 페이징: WHERE user_id=? ORDER BY created_at DESC
-- (before) type=ALL, rows=50000 / (after) type=ref, rows=23
CREATE INDEX idx_res_user_created       ON reservations (user_id, created_at);

-- 스타일리스트 슬롯 조회 + 분산락 중복 체크
-- WHERE stylist_id=? AND reserved_at=? AND status IN (?)
CREATE INDEX idx_res_stylist_reserved   ON reservations (stylist_id, reserved_at);

-- 랭킹 집계: WHERE stylist_id=? AND reserved_at>=? AND status IN (?)
CREATE INDEX idx_res_stylist_status_reserved ON reservations (stylist_id, status, reserved_at);

-- 리마인더 스케줄러: WHERE reserved_at BETWEEN ? AND ? AND status=?
CREATE INDEX idx_res_reserved_status    ON reservations (reserved_at, status);


-- ── reviews ───────────────────────────────────────────────────
-- 미용사 리뷰 목록: WHERE stylist_id=? ORDER BY created_at DESC
-- (before) type=ALL, rows=30000 / (after) type=ref, rows=12
CREATE INDEX idx_review_stylist_created ON reviews (stylist_id, created_at);

-- 내 리뷰 목록: WHERE user_id=?
CREATE INDEX idx_review_user            ON reviews (user_id);


-- ── stylist_profiles ──────────────────────────────────────────
-- 평점순 정렬: ORDER BY rating DESC
CREATE INDEX idx_stylist_rating         ON stylist_profiles (rating);

-- 리뷰순 정렬: ORDER BY review_count DESC
CREATE INDEX idx_stylist_review_count   ON stylist_profiles (review_count);


-- ── salons ────────────────────────────────────────────────────
-- 지역별 랭킹: WHERE district=?
-- (before) type=ALL, rows=5000 / (after) type=ref, rows=300
CREATE INDEX idx_salon_district         ON salons (district);

-- 미용실명 검색 (prefix LIKE '강남%' 패턴에서만 유효, '%강남%' 패턴 불가)
CREATE INDEX idx_salon_name             ON salons (name);

-- 위치 기반 반경 검색: ST_Distance_Sphere(location, ?)
-- SPATIAL INDEX는 NOT NULL + SRID 컬럼에만 생성 가능
-- ddl-auto로 생성 불가 → 직접 실행 필요
ALTER TABLE salons MODIFY COLUMN location POINT NOT NULL SRID 4326;
CREATE SPATIAL INDEX idx_salon_location ON salons (location);
