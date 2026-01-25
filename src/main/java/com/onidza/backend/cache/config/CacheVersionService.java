package com.onidza.backend.cache.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CacheVersionService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> INCRBY_SET_TTL_IF_NO_TTL =
            new DefaultRedisScript<>(
                    """
                            local v = redis.call('INCRBY', KEYS[1], ARGV[1])
                            local ttl = redis.call('TTL', KEYS[1])
                            if ttl < 0 then
                              redis.call('EXPIRE', KEYS[1], ARGV[2])
                            end
                            return v
                            """,
                    Long.class
            );

    public long getKeyVersion(String key) {
        long ttlSeconds = Duration.ofMinutes(2).toSeconds();
        Long ver = stringRedisTemplate.execute(
                INCRBY_SET_TTL_IF_NO_TTL,
                List.of(key),
                "0",
                String.valueOf(ttlSeconds)
        );

        return ver == null ? 0L : ver;
    }

    public void bumpVersion(String key) {
        long ttlSeconds = Duration.ofMinutes(2).toSeconds();

        stringRedisTemplate.execute(
                INCRBY_SET_TTL_IF_NO_TTL,
                List.of(key),
                "1",
                String.valueOf(ttlSeconds)
        );
    }
}
