-- salons 테이블에 district 컬럼 추가 후 기존 데이터 업데이트
-- (JPA ddl-auto: update 로 컬럼은 자동 생성됨, 값만 채워주면 됨)

UPDATE salons SET district = '강남구' WHERE name = 'D.E Studio';
UPDATE salons SET district = '용산구' WHERE name = 'Max Studio';
UPDATE salons SET district = '마포구' WHERE name = 'JUNO Hair';
