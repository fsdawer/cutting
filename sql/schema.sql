-- ============================================================
-- BeautyBook DB Schema (DDL)
-- OAuth: 네이버, 카카오
-- Payment: 토스페이먼츠 (토스, 네이버페이, 카카오페이)
-- Email: 네이버 SMTP 인증
-- ============================================================

CREATE DATABASE IF NOT EXISTS beautybook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE beautybook;

-- ============================================================
-- 1. 회원
-- ============================================================
CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT       COMMENT '회원 고유 PK',
    username    VARCHAR(50)     NOT NULL COMMENT '자사 로그인 아이디 (소셜 로그인 시 provider_id 기반으로 자동 생성)',
    email       VARCHAR(100)    NOT NULL COMMENT '이메일 (인증 및 알림용)',
    password    VARCHAR(255)    NULL     COMMENT '비밀번호 (BCrypt 암호화 저장, 소셜 로그인 시 NULL)',
    name        VARCHAR(50)     NOT NULL COMMENT '실명 혹은 닉네임',
    phone       VARCHAR(20)     NULL     COMMENT '휴대폰 번호',
    role        ENUM('USER','STYLIST') NOT NULL DEFAULT 'USER' COMMENT '권한 (USER=일반고객, STYLIST=미용사)',
    profile_img VARCHAR(500)    NULL     COMMENT '프로필 이미지 URL (NCP Object Storage)',

    -- 소셜 로그인
    provider    ENUM('LOCAL','KAKAO','NAVER') NOT NULL DEFAULT 'LOCAL' COMMENT '로그인 제공자 (LOCAL=자사, KAKAO, NAVER)',
    provider_id VARCHAR(200)    NULL COMMENT '소셜 로그인 제공자가 발급한 고유 ID',

    -- 이메일 인증
    is_verified   TINYINT(1)    NOT NULL DEFAULT 0   COMMENT '이메일 인증 완료 여부 (0=미인증, 1=인증완료)',

    -- 토큰
    refresh_token VARCHAR(500)  NULL                 COMMENT 'Refresh Token (로그아웃 시 NULL로 초기화)',

    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email    (email),
    UNIQUE KEY uq_users_provider (provider, provider_id)
) COMMENT '회원';

-- ============================================================
-- 2. 이메일 인증 토큰
-- ============================================================
CREATE TABLE email_verifications (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '인증 토큰 고유 PK',
    email       VARCHAR(100)    NOT NULL                 COMMENT '인증 대상 이메일',
    token       VARCHAR(64)     NOT NULL COMMENT '인증 토큰 (UUID 또는 6자리 코드)',
    expired_at  DATETIME        NOT NULL COMMENT '토큰 만료 시각 (발급 후 10분)',
    is_used     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '토큰 사용 여부 (0=미사용, 1=사용됨)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '토큰 발급일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_ev_token (token),
    INDEX idx_ev_email (email)
) COMMENT '이메일 인증';

-- ============================================================
-- 3. 미용실
-- ============================================================
CREATE TABLE salons (
    id          BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '미용실 고유 PK',
    name        VARCHAR(100)    NOT NULL                         COMMENT '미용실 이름',
    address     VARCHAR(200)    NULL                             COMMENT '미용실 주소',
    phone       VARCHAR(20)     NULL                             COMMENT '미용실 전화번호',
    description TEXT            NULL                             COMMENT '미용실 소개',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    PRIMARY KEY (id)
) COMMENT '미용실';

-- ============================================================
-- 4. 미용사 프로필 (role=STYLIST 인 user 와 1:1)
-- ============================================================
CREATE TABLE stylist_profiles (
    id           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '미용사 프로필 고유 PK',
    user_id      BIGINT          NOT NULL               COMMENT 'users.id 참조 (role=STYLIST인 회원)',
    salon_id     BIGINT          NULL                   COMMENT 'salons.id 참조 (소속 미용실)',
    bio          TEXT            NULL                   COMMENT '자기소개 및 스타일링 소개',
    experience   INT             NOT NULL DEFAULT 0     COMMENT '경력 연수',
    rating       DECIMAL(3,1)    NOT NULL DEFAULT 0.0   COMMENT '평점 캐싱 (리뷰 작성 시 갱신, 0.0~5.0)',
    review_count INT             NOT NULL DEFAULT 0     COMMENT '리뷰 수 캐싱 (리뷰 작성 시 갱신)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_sp_user_id (user_id),
    CONSTRAINT fk_sp_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_sp_salon FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE SET NULL
) COMMENT '미용사 프로필';

-- ============================================================
-- 4. 미용사 서비스/가격표
-- ============================================================
CREATE TABLE stylist_services (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '서비스 항목 고유 PK',
    stylist_id  BIGINT          NOT NULL               COMMENT 'stylist_profiles.id 참조',
    name        VARCHAR(100)    NOT NULL COMMENT '서비스명 (커트, 펌 등)',
    category    VARCHAR(50)     NULL     COMMENT '서비스 카테고리 (커트, 펌, 염색, 케어, 기타)',
    price       INT             NOT NULL COMMENT '서비스 가격 (원)',
    duration    INT             NOT NULL COMMENT '서비스 소요시간 (분)',
    description VARCHAR(255)    NULL     COMMENT '서비스 설명',
    is_active   TINYINT(1)      NOT NULL DEFAULT 1     COMMENT '서비스 활성 여부 (0=비활성, 1=활성)',

    PRIMARY KEY (id),
    INDEX idx_ss_stylist (stylist_id),
    CONSTRAINT fk_ss_stylist FOREIGN KEY (stylist_id) REFERENCES stylist_profiles(id) ON DELETE CASCADE
) COMMENT '미용사 서비스';

-- ============================================================
-- 5. 영업시간
-- ============================================================
CREATE TABLE operating_hours (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '영업시간 고유 PK',
    stylist_id   BIGINT      NOT NULL               COMMENT 'stylist_profiles.id 참조',
    day_of_week  TINYINT     NOT NULL COMMENT '요일 (0=월, 1=화, 2=수, 3=목, 4=금, 5=토, 6=일)',
    open_time    TIME        NULL                   COMMENT '영업 시작 시각 (is_closed=1 이면 NULL)',
    close_time   TIME        NULL                   COMMENT '영업 종료 시각 (is_closed=1 이면 NULL)',
    is_closed    TINYINT(1)  NOT NULL DEFAULT 0     COMMENT '휴무 여부 (0=영업, 1=휴무)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_oh_stylist_day (stylist_id, day_of_week),
    CONSTRAINT fk_oh_stylist FOREIGN KEY (stylist_id) REFERENCES stylist_profiles(id) ON DELETE CASCADE
) COMMENT '영업시간';

-- ============================================================
-- 6. 포트폴리오
-- ============================================================
CREATE TABLE portfolios (
    id          BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '포트폴리오 이미지 고유 PK',
    stylist_id  BIGINT          NOT NULL                         COMMENT 'stylist_profiles.id 참조',
    image_url   VARCHAR(500)    NOT NULL                         COMMENT '포트폴리오 이미지 URL (NCP Object Storage)',
    caption     VARCHAR(200)    NULL                             COMMENT '이미지 설명 (선택 입력)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이미지 등록일시',

    PRIMARY KEY (id),
    INDEX idx_pf_stylist (stylist_id),
    CONSTRAINT fk_pf_stylist FOREIGN KEY (stylist_id) REFERENCES stylist_profiles(id) ON DELETE CASCADE
) COMMENT '포트폴리오';

-- ============================================================
-- 7. 예약
-- ============================================================
CREATE TABLE reservations (
    id           BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '예약 고유 PK',
    user_id      BIGINT          NOT NULL                         COMMENT '예약한 고객 (users.id 참조)',
    stylist_id   BIGINT          NOT NULL                         COMMENT '예약 대상 미용사 (stylist_profiles.id 참조)',
    service_id   BIGINT          NOT NULL                         COMMENT '예약한 서비스 (stylist_services.id 참조)',
    reserved_at  DATETIME        NOT NULL COMMENT '예약 날짜 및 시작 시각',
    status       ENUM('PENDING','CONFIRMED','DONE','CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '예약 상태 (PENDING=대기, CONFIRMED=확정, DONE=완료, CANCELLED=취소)',
    request_memo TEXT            NULL COMMENT '고객 요청사항 (원하는 스타일 등)',
    total_price  INT             NOT NULL                         COMMENT '최종 결제 금액 (원)',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '예약 생성일시',

    PRIMARY KEY (id),
    INDEX idx_rv_user    (user_id),
    INDEX idx_rv_stylist (stylist_id),
    CONSTRAINT fk_rv_user    FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_rv_stylist FOREIGN KEY (stylist_id) REFERENCES stylist_profiles(id),
    CONSTRAINT fk_rv_service FOREIGN KEY (service_id) REFERENCES stylist_services(id)
) COMMENT '예약';

-- ============================================================
-- 8. 예약 참고 이미지
-- ============================================================
CREATE TABLE reservation_images (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '예약 참고 이미지 고유 PK',
    reservation_id  BIGINT          NOT NULL               COMMENT '예약 (reservations.id 참조)',
    image_url       VARCHAR(500)    NOT NULL               COMMENT '참고 이미지 URL (고객이 업로드한 스타일 참고 사진)',

    PRIMARY KEY (id),
    INDEX idx_ri_reservation (reservation_id),
    CONSTRAINT fk_ri_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
) COMMENT '예약 참고 이미지';

-- ============================================================
-- 9. 결제
-- 토스페이먼츠 기준: payment_key + order_id
-- method로 토스/네이버페이/카카오페이 구분
-- ============================================================
CREATE TABLE payments (
    id              BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '결제 고유 PK',
    reservation_id  BIGINT          NOT NULL                         COMMENT '결제 대상 예약 (reservations.id 참조)',
    order_id        VARCHAR(100)    NOT NULL COMMENT '우리 측에서 생성한 주문 ID (UUID, 토스페이먼츠 요청 시 사용)',
    payment_key     VARCHAR(200)    NULL     COMMENT '토스페이먼츠가 발급한 결제 고유 키 (결제 승인/취소 시 사용)',
    amount          INT             NOT NULL                         COMMENT '결제 금액 (원)',
    method          ENUM('TOSS','NAVER_PAY','KAKAO_PAY') NOT NULL    COMMENT '결제 수단 (TOSS=토스, NAVER_PAY=네이버페이, KAKAO_PAY=카카오페이)',
    status          ENUM('PENDING','PAID','REFUNDED')    NOT NULL DEFAULT 'PENDING' COMMENT '결제 상태 (PENDING=대기, PAID=완료, REFUNDED=환불)',
    paid_at         DATETIME        NULL                             COMMENT '실제 결제 완료 시각 (미결제 시 NULL)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '결제 레코드 생성일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_pm_reservation (reservation_id),
    UNIQUE KEY uq_pm_order_id    (order_id),
    CONSTRAINT fk_pm_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) COMMENT '결제';

-- ============================================================
-- 10. 채팅방 (예약 확정 시 자동 생성)
-- ============================================================
CREATE TABLE chat_rooms (
    id                   BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '채팅방 고유 PK',
    reservation_id       BIGINT       NOT NULL                         COMMENT '연결된 예약 (reservations.id 참조, 예약 1개당 채팅방 1개)',
    user_id              BIGINT       NOT NULL COMMENT '채팅 참여 고객 (users.id 참조)',
    stylist_user_id      BIGINT       NOT NULL COMMENT '채팅 참여 미용사 (users.id 참조, stylist_profiles가 아닌 users 직접 참조)',
    last_message_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '마지막 메시지 전송 시각 (채팅방 목록 최신순 정렬 기준)',
    last_message_content VARCHAR(300) NULL     COMMENT '마지막 메시지 내용 미리보기',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '채팅방 생성일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_cr_reservation (reservation_id),
    CONSTRAINT fk_cr_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_cr_user        FOREIGN KEY (user_id)         REFERENCES users(id),
    CONSTRAINT fk_cr_stylist     FOREIGN KEY (stylist_user_id) REFERENCES users(id)
) COMMENT '채팅방';

-- ============================================================
-- 11. 채팅 메시지
-- ============================================================
CREATE TABLE messages (
    id          BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '메시지 고유 PK',
    room_id     BIGINT          NOT NULL                         COMMENT '메시지가 속한 채팅방 (chat_rooms.id 참조)',
    sender_id   BIGINT          NOT NULL                         COMMENT '메시지 발신자 (users.id 참조)',
    content     TEXT            NULL                             COMMENT '텍스트 메시지 내용 (이미지 전송 시 NULL 가능)',
    image_url   VARCHAR(500)    NULL                             COMMENT '이미지 메시지 URL (텍스트 전송 시 NULL)',
    is_read     TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '읽음 여부 (0=안읽음, 1=읽음)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 전송일시',

    PRIMARY KEY (id),
    INDEX idx_msg_room   (room_id),
    INDEX idx_msg_sender (sender_id),
    CONSTRAINT fk_msg_room   FOREIGN KEY (room_id)   REFERENCES chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id)
) COMMENT '채팅 메시지';

-- ============================================================
-- 12. 리뷰 (예약 1건당 1개)
-- ============================================================
CREATE TABLE reviews (
    id              BIGINT      NOT NULL AUTO_INCREMENT          COMMENT '리뷰 고유 PK',
    reservation_id  BIGINT      NOT NULL                         COMMENT '리뷰 대상 예약 (reservations.id 참조, 예약 1개당 리뷰 1개)',
    user_id         BIGINT      NOT NULL                         COMMENT '리뷰 작성 고객 (users.id 참조)',
    stylist_id      BIGINT      NOT NULL                         COMMENT '리뷰 대상 미용사 (stylist_profiles.id 참조)',
    rating          TINYINT     NOT NULL COMMENT '별점 (1~5)',
    content         TEXT        NULL                             COMMENT '리뷰 본문 내용 (선택 입력)',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '리뷰 작성일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_rv_reservation (reservation_id),
    INDEX idx_rev_stylist (stylist_id),
    CONSTRAINT fk_rev_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_rev_user        FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_rev_stylist     FOREIGN KEY (stylist_id) REFERENCES stylist_profiles(id)
) COMMENT '리뷰';
