# ── 1단계: 빌드 ──────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar -x test

# ── 2단계: 실행 이미지 ────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir -p uploads/reservation-images

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
