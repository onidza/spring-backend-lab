package com.onidza.backend.config.manual;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheVersionService {

    private final StringRedisTemplate stringRedisTemplate;

    public long getKeyVersion(String key) {
        Long ver = stringRedisTemplate.opsForValue()
                .increment(key, 0);

        return ver == null ? 0 : ver;
    }

    public void bumpVersion(String key) {
        stringRedisTemplate.opsForValue()
                .increment(key);
    }
}
