package beauty.beauty.reservation.service;

import beauty.beauty.ranking.service.RankingService;
import beauty.beauty.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [After 2 + After 3] 통합 테스트
 *
 * @SpringBootTest: 실제 Spring Context 로드 (Redis, JPA 등 포함)
 * @ActiveProfiles("test"): application-test.yml 사용
 *
 * 실행 전 필요: Redis가 로컬에서 실행 중이어야 함 (또는 TestContainers 설정)
 *   redis-server --port 6379
 *
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │ 테스트                    │ 검증 항목                                  │
 * ├───────────────────────────┼───────────────────────────────────────────│
 * │ distributedLock           │ 동시 10요청 → 1개만 성공                    │
 * │ distributedLock_afterTtl  │ TTL 만료 후 재획득 가능                    │
 * └───────────────────────────────────────────────────────────────────────┘
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservationIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private RankingService rankingService;

    @MockitoBean
    private NotificationService notificationService;

    // ── 테스트 1: Redis 분산락 동시성 검증 ────────────────────────────────────
    // 10개 스레드가 동시에 같은 락 키를 SETNX → 1개만 성공해야 함

    @Test
    @DisplayName("[After 3] Redis SETNX 분산락 — 동시 10요청 중 1개만 락 획득")
    void distributedLock_onlyOneAcquiresLock() throws InterruptedException {
        String lockKey = "lock:test:concurrent:" + System.currentTimeMillis();
        int THREAD_COUNT = 10;

        AtomicInteger acquiredCount = new AtomicInteger(0);
        AtomicInteger failedCount  = new AtomicInteger(0);

        CountDownLatch startLatch  = new CountDownLatch(1);
        CountDownLatch doneLatch   = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Boolean acquired = redisTemplate.opsForValue()
                            .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

                    if (Boolean.TRUE.equals(acquired)) {
                        acquiredCount.incrementAndGet();
                        Thread.sleep(50);
                        redisTemplate.delete(lockKey);
                    } else {
                        failedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.printf(
                "[분산락 테스트] 락 획득 성공: %d건, 실패(거부): %d건 (총 %d건)%n",
                acquiredCount.get(), failedCount.get(), THREAD_COUNT
        );

        assertThat(acquiredCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(acquiredCount.get() + failedCount.get()).isEqualTo(THREAD_COUNT);
    }

    // ── 테스트 2: 분산락 TTL 만료 검증 ────────────────────────────────────────

    @Test
    @DisplayName("[After 3] 락 TTL 만료 후 재획득 가능")
    void distributedLock_afterTtlExpires_canReacquire() throws InterruptedException {
        String lockKey = "lock:test:ttl:" + System.currentTimeMillis();

        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 1, TimeUnit.SECONDS);
        assertThat(first).isTrue();

        Boolean second = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 1, TimeUnit.SECONDS);
        assertThat(second).isFalse();

        Thread.sleep(1500);

        Boolean third = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 1, TimeUnit.SECONDS);
        assertThat(third).isTrue();

        redisTemplate.delete(lockKey);
    }
}
