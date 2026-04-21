# CutIng 프로젝트 (beauty)

## 기술 스택
- **Backend**: Spring Boot 4.0.3 / Java 17 / JPA + MySQL
- **Frontend**: Vue.js 3 (Vite) + Pinia + Vue Router
- **Auth**: JWT (`io.jsonwebtoken:jjwt 0.12.6`) + Spring Security + OAuth2 (Google)
- **외부 API**: 토스페이먼츠 v2, 네이버 SMTP
- **기타**: WebSocket(채팅), spring-dotenv(환경변수), Lombok

## 프로젝트 폴더 구조

```
beauty/
├── CLAUDE.md
├── README.md
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── .env                          # 절대 커밋 금지 (.gitignore 처리됨)
├── sql/                          # DDL/초기 데이터 SQL 스크립트
│
├── src/
│   ├── main/
│   │   ├── java/beauty/beauty/
│   │   │   ├── BeautyApplication.java
│   │   │   ├── auth/
│   │   │   │   ├── controller/AuthController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── AuthServiceImpl.java
│   │   │   │   │   └── CustomOAuth2UserService.java
│   │   │   │   └── dto/
│   │   │   │       ├── RegisterRequest.java
│   │   │   │       ├── LoginRequest.java
│   │   │   │       ├── EmailRequest.java
│   │   │   │       └── TokenResponse.java
│   │   │   ├── chat/
│   │   │   │   ├── controller/ChatController.java
│   │   │   │   ├── service/ChatService.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── ChatRoom.java
│   │   │   │   │   └── Message.java
│   │   │   │   └── repository/
│   │   │   │       ├── ChatRoomRepository.java
│   │   │   │       └── MessageRepository.java
│   │   │   ├── global/
│   │   │   │   ├── annotation/LoginUserId.java       # 컨트롤러 파라미터 리졸버용
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── WebConfig.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── CustomException.java
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── jwt/
│   │   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   │   └── JwtUtil.java
│   │   │   │   ├── oauth2/OAuth2SuccessHandler.java
│   │   │   │   └── resolver/LoginUserIdResolver.java
│   │   │   ├── payment/
│   │   │   │   ├── controller/PaymentController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── PaymentService.java
│   │   │   │   │   ├── PaymentServiceImpl.java
│   │   │   │   │   └── PaymentCleanupScheduler.java  # PENDING 10분 후 자동 삭제
│   │   │   │   ├── entity/Payment.java               # PayStatus(PENDING→PAID→REFUNDED)
│   │   │   │   ├── repository/PaymentRepository.java
│   │   │   │   └── dto/
│   │   │   │       ├── PaymentPrepareRequest.java
│   │   │   │       ├── PaymentPrepareResponse.java
│   │   │   │       ├── PaymentConfirmRequest.java
│   │   │   │       ├── PaymentResponse.java
│   │   │   │       └── RefundRequest.java
│   │   │   ├── reservation/
│   │   │   │   ├── controller/ReservationController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── ReservationService.java
│   │   │   │   │   └── ReservationServiceImpl.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Reservation.java              # status/createdAt에 @Builder.Default 적용
│   │   │   │   │   └── ReservationImage.java
│   │   │   │   ├── repository/ReservationRepository.java
│   │   │   │   └── dto/
│   │   │   │       ├── ReservationRequest.java
│   │   │   │       └── ReservationResponse.java
│   │   │   ├── review/
│   │   │   │   ├── controller/ReviewController.java
│   │   │   │   ├── service/ReviewService.java
│   │   │   │   ├── entity/Review.java
│   │   │   │   └── repository/ReviewRepository.java
│   │   │   ├── stylist/
│   │   │   │   ├── controller/StylistController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── StylistService.java
│   │   │   │   │   └── StylistServiceImpl.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── StylistProfile.java
│   │   │   │   │   ├── StylistServiceItem.java
│   │   │   │   │   ├── OperatingHours.java
│   │   │   │   │   └── Portfolio.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── StylistProfileRepository.java
│   │   │   │   │   ├── StylistServiceRepository.java
│   │   │   │   │   ├── OperatingHoursRepository.java
│   │   │   │   │   └── PortfolioRepository.java
│   │   │   │   └── dto/
│   │   │   │       ├── StylistProfileResponse.java
│   │   │   │       ├── ServiceRequest.java
│   │   │   │       ├── ServiceResponse.java
│   │   │   │       ├── WorkingHoursRequest.java
│   │   │   │       ├── WorkingHoursResponse.java
│   │   │   │       ├── PortfolioResponse.java
│   │   │   │       └── UpdateStylistProfileRequest.java
│   │   │   └── user/
│   │   │       ├── controller/UserController.java
│   │   │       ├── service/
│   │   │       │   ├── UserService.java
│   │   │       │   └── UserServiceImpl.java
│   │   │       ├── entity/
│   │   │       │   ├── User.java
│   │   │       │   └── EmailVerification.java
│   │   │       ├── repository/
│   │   │       │   ├── UserRepository.java
│   │   │       │   └── EmailVerificationRepository.java
│   │   │       └── dto/
│   │   │           ├── UserResponse.java
│   │   │           ├── UpdateUserRequest.java
│   │   │           ├── ChangePasswordRequest.java
│   │   │           └── UpgradeToStylistRequest.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/beauty/beauty/
│           ├── BeautyApplicationTests.java
│           └── stylist/service/StylistServiceImplIntegrationTest.java
│
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── style.css
        ├── assets/main.css
        ├── api/
        │   ├── index.js          # axios 인스턴스 + 공통 인터셉터
        │   ├── auth.js
        │   ├── user.js
        │   ├── stylist.js
        │   ├── reservation.js
        │   └── payment.js
        ├── stores/
        │   └── authStore.js      # Pinia - JWT 토큰/유저 상태 관리
        ├── router/
        │   └── index.js          # Vue Router (navigation guard 포함)
        ├── components/
        │   ├── Navbar.vue
        │   └── StylistCard.vue
        └── views/
            ├── HomeView.vue
            ├── LoginView.vue
            ├── RegisterView.vue
            ├── OAuth2CallbackView.vue
            ├── MyPageView.vue
            ├── StylistDetailView.vue
            ├── StylistManageView.vue
            ├── StylistReservationsView.vue
            ├── BookingView.vue
            ├── PaymentView.vue
            ├── PaymentSuccessView.vue
            ├── PaymentFailView.vue
            ├── ChatView.vue
            └── NotFoundView.vue
```

## 빌드 & 테스트

```bash
# Backend (루트에서)
./gradlew build          # 빌드 + 테스트
./gradlew test           # 테스트만
./gradlew bootRun        # 개발 서버 실행 (포트 8080)

# Frontend (frontend/ 디렉토리에서)
npm install
npm run dev              # 개발 서버 실행 (포트 5173)
npm run build            # 프로덕션 빌드
```

## 도메인 요약

### 결제 (payment)
- `Payment` 엔티티: `PayStatus(PENDING→PAID→REFUNDED)`, `Method(TOSS/NAVER_PAY/KAKAO_PAY)`
- `Payment`와 `Reservation`은 `@OneToOne` 관계
- 플로우: `prepare(PENDING 생성)` → 프론트 토스 위젯 → `confirm(PAID)` → 필요시 `refund(REFUNDED)`
- PENDING 상태 10분 후 `PaymentCleanupScheduler`가 자동 삭제
- `HttpClient`는 `PaymentServiceImpl`의 인스턴스 변수로 관리 (재사용)

### 예약 (reservation)
- `Reservation`은 `User`, `StylistServiceItem` 참조
- `totalPrice` 필드가 결제 금액 기준

### 인증
- 컨트롤러에서 `@LoginUserId Long userId`로 현재 로그인 유저 ID 주입
- JWT 토큰은 HTTP Authorization 헤더로 전달

## 예외 처리 컨벤션
- `CustomException(ErrorCode)` → `GlobalExceptionHandler` → `{code, message}` JSON
- `IllegalArgumentException` → 400 BAD_REQUEST
- `IllegalStateException` → 409 CONFLICT
- `ErrorCode` enum에 HTTP 상태 + 메시지 정의됨

## API 베이스 경로
- `/api/auth/**`
- `/api/users/**`
- `/api/stylists/**`
- `/api/reservations/**`
- `/api/payments/**`
- `/api/reviews/**`
- `/api/chat/**`

## 환경변수 (.env) — 커밋 금지
- `toss.secret-key` → 토스페이먼츠 시크릿 키
- DB, JWT, OAuth2, SMTP 설정 포함
- `spring-dotenv` 라이브러리로 `.env` → Spring 환경변수 자동 주입

## 주의사항 / 알려진 패턴
- `@LoginUserId`는 커스텀 애노테이션 + `HandlerMethodArgumentResolver`로 동작
- **`@Builder` 주의**: 필드 선언부 기본값(`= LocalDateTime.now()` 등)은 `@Builder.Default` 없이는 무시됨
- `Reservation` 엔티티: `status`, `createdAt`에 `@Builder.Default` 적용됨
- 결제 위젯 플로우: `prepare()` → 토스 SDK `renderPaymentMethods` → `requestPayment()` → 리다이렉트 → `/payment/success` 또는 `/payment/fail`
- SDK 없는 개발환경: prepare 후 mock paymentKey로 confirm 직접 호출
- `getMyPayments`: `reservationRepository.findByUserId` → `paymentRepository.findByReservationIdIn` 2-query 패턴
