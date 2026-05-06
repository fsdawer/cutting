-- 랭킹 병목 테스트용 데이터
-- 강남구에 미용사 20명 추가 → N+1 쿼리가 41번 발생하도록

-- 1. 강남구 살롱 20개
INSERT INTO salons (name, address, district, phone, description) VALUES
('강남 헤어01', '강남구 역삼동 1번지', '강남구', '02-0001-0001', '테스트 살롱'),
('강남 헤어02', '강남구 역삼동 2번지', '강남구', '02-0001-0002', '테스트 살롱'),
('강남 헤어03', '강남구 역삼동 3번지', '강남구', '02-0001-0003', '테스트 살롱'),
('강남 헤어04', '강남구 역삼동 4번지', '강남구', '02-0001-0004', '테스트 살롱'),
('강남 헤어05', '강남구 역삼동 5번지', '강남구', '02-0001-0005', '테스트 살롱'),
('강남 헤어06', '강남구 역삼동 6번지', '강남구', '02-0001-0006', '테스트 살롱'),
('강남 헤어07', '강남구 역삼동 7번지', '강남구', '02-0001-0007', '테스트 살롱'),
('강남 헤어08', '강남구 역삼동 8번지', '강남구', '02-0001-0008', '테스트 살롱'),
('강남 헤어09', '강남구 역삼동 9번지', '강남구', '02-0001-0009', '테스트 살롱'),
('강남 헤어10', '강남구 역삼동 10번지','강남구', '02-0001-0010', '테스트 살롱'),
('강남 헤어11', '강남구 역삼동 11번지','강남구', '02-0001-0011', '테스트 살롱'),
('강남 헤어12', '강남구 역삼동 12번지','강남구', '02-0001-0012', '테스트 살롱'),
('강남 헤어13', '강남구 역삼동 13번지','강남구', '02-0001-0013', '테스트 살롱'),
('강남 헤어14', '강남구 역삼동 14번지','강남구', '02-0001-0014', '테스트 살롱'),
('강남 헤어15', '강남구 역삼동 15번지','강남구', '02-0001-0015', '테스트 살롱'),
('강남 헤어16', '강남구 역삼동 16번지','강남구', '02-0001-0016', '테스트 살롱'),
('강남 헤어17', '강남구 역삼동 17번지','강남구', '02-0001-0017', '테스트 살롱'),
('강남 헤어18', '강남구 역삼동 18번지','강남구', '02-0001-0018', '테스트 살롱'),
('강남 헤어19', '강남구 역삼동 19번지','강남구', '02-0001-0019', '테스트 살롱'),
('강남 헤어20', '강남구 역삼동 20번지','강남구', '02-0001-0020', '테스트 살롱');

-- 2. 미용사 계정 20개 (password: password123)
INSERT INTO users (username, email, password, name, phone, role, provider, is_verified) VALUES
('test_stylist01','ts01@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사01','010-0001-0001','STYLIST','LOCAL',1),
('test_stylist02','ts02@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사02','010-0001-0002','STYLIST','LOCAL',1),
('test_stylist03','ts03@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사03','010-0001-0003','STYLIST','LOCAL',1),
('test_stylist04','ts04@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사04','010-0001-0004','STYLIST','LOCAL',1),
('test_stylist05','ts05@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사05','010-0001-0005','STYLIST','LOCAL',1),
('test_stylist06','ts06@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사06','010-0001-0006','STYLIST','LOCAL',1),
('test_stylist07','ts07@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사07','010-0001-0007','STYLIST','LOCAL',1),
('test_stylist08','ts08@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사08','010-0001-0008','STYLIST','LOCAL',1),
('test_stylist09','ts09@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사09','010-0001-0009','STYLIST','LOCAL',1),
('test_stylist10','ts10@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사10','010-0001-0010','STYLIST','LOCAL',1),
('test_stylist11','ts11@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사11','010-0001-0011','STYLIST','LOCAL',1),
('test_stylist12','ts12@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사12','010-0001-0012','STYLIST','LOCAL',1),
('test_stylist13','ts13@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사13','010-0001-0013','STYLIST','LOCAL',1),
('test_stylist14','ts14@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사14','010-0001-0014','STYLIST','LOCAL',1),
('test_stylist15','ts15@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사15','010-0001-0015','STYLIST','LOCAL',1),
('test_stylist16','ts16@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사16','010-0001-0016','STYLIST','LOCAL',1),
('test_stylist17','ts17@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사17','010-0001-0017','STYLIST','LOCAL',1),
('test_stylist18','ts18@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사18','010-0001-0018','STYLIST','LOCAL',1),
('test_stylist19','ts19@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사19','010-0001-0019','STYLIST','LOCAL',1),
('test_stylist20','ts20@test.com','$2a$10$N6WtEn1kL5tEm5fGV0Hm5eGzfQ2hS8rQ3T5Y6B1Z4W9X7V2I0J3','테스트미용사20','010-0001-0020','STYLIST','LOCAL',1);

-- 3. 미용사 프로필 연결 (user_id, salon_id는 방금 insert된 것)
INSERT INTO stylist_profiles (user_id, salon_id, bio, experience, rating, review_count)
SELECT u.id, s.id, '테스트 미용사입니다.', 3, 4.0, 0
FROM users u
JOIN salons s ON s.name = CONCAT('강남 헤어', LPAD(SUBSTRING(u.username, 13), 2, '0'))
WHERE u.username LIKE 'test_stylist%';

-- 4. 각 미용사에게 리뷰 30개씩 추가 (N+1 쿼리 부하용)
-- reservation이 없으면 review 제약 때문에 direct insert가 어려우므로
-- 리뷰 없이 미용사만 있어도 N+1은 발생 (리뷰 쿼리가 빈 결과를 반환하는 쿼리를 N번 날림)
-- 아래는 reservation + review 대신 rating/review_count를 직접 세팅
UPDATE stylist_profiles sp
JOIN users u ON sp.user_id = u.id
SET sp.rating = ROUND(3.5 + RAND() * 1.5, 1),
    sp.review_count = FLOOR(10 + RAND() * 90)
WHERE u.username LIKE 'test_stylist%';

-- 확인
SELECT COUNT(*) as 강남구_미용사수 FROM stylist_profiles sp
JOIN salons s ON sp.salon_id = s.id
WHERE s.district = '강남구';
