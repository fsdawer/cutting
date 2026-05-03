package beauty.beauty.global.config;

import beauty.beauty.global.redis.RedisMessageSubscriber;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(redisObjectMapper());
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(jsonPair)
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "booked_times", defaults.entryTtl(Duration.ofMinutes(30)),
                        "chat_rooms",   defaults.entryTtl(Duration.ofMinutes(1)),
                        "stylist",      defaults.entryTtl(Duration.ofHours(1)),
                        "stylists",     defaults.entryTtl(Duration.ofHours(1))
                ))
                .build();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            RedisMessageSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber, new PatternTopic("chat:room:*"));
        return container;
    }
}
