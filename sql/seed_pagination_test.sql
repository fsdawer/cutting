-- ============================================================
-- 페이지네이션 성능 테스트용 대량 데이터 생성 스크립트
-- 실행 전제: user_id=1, stylist_id=1, service_id=1 이 존재해야 함
-- 실행 방법: mysql -u root -p beautydb < sql/seed_pagination_test.sql
-- ============================================================

-- 재실행 시 중복 방지
DELETE FROM reservations WHERE user_id = 17 AND request_memo LIKE 'PAGTEST%';

-- Stored Procedure로 5만 건 INSERT (직접 루프)
DROP PROCEDURE IF EXISTS seed_reservations;

DELIMITER $$
CREATE PROCEDURE seed_reservations()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 50000 DO
        INSERT INTO reservations (user_id, stylist_id, service_id, reserved_at, status, total_price, created_at, request_memo)
        VALUES (
            17,
            1,
            1,
            DATE_ADD('2023-01-01 10:00:00', INTERVAL i HOUR),
            ELT(1 + FLOOR(RAND() * 4), 'PENDING', 'CONFIRMED', 'DONE', 'CANCELLED'),
            50000 + FLOOR(RAND() * 100000),
            DATE_ADD('2023-01-01 09:00:00', INTERVAL i HOUR),
            CONCAT('PAGTEST-', i)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_reservations();
DROP PROCEDURE IF EXISTS seed_reservations;

SELECT COUNT(*) AS total_reservations FROM reservations WHERE user_id = 17;
