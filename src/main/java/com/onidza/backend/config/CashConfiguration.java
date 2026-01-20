package com.onidza.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class CashConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisObjTemplate(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        var defaultConf = base.entryTtl(Duration.ofMinutes(1));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("client", base.entryTtl(Duration.ofMinutes(1)));
        perCache.put("clientPage", base.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConf)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }

    @Bean
    public KeyGenerator clientPageKeyGen() {
        return (target, method, params) -> {
            int page = (int) params[0];
            int size = (int) params[1];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            return "p=" + safePage + ":s=" + safeSize;
        };
    }
}
