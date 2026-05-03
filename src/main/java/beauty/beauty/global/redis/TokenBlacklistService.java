package beauty.beauty.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "blacklist:";

    public void blacklist(String token, long remainingMs) {
        if (remainingMs > 0) {
            stringRedisTemplate.opsForValue()
                    .set(PREFIX + token, "1", Duration.ofMillis(remainingMs));
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PREFIX + token));
    }
}
