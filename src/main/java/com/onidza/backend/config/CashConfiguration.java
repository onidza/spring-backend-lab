package com.onidza.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
        perCache.put(CacheKeys.CLIENT_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.CLIENTS_PAGE_VER_KEY, base.entryTtl(Duration.ofSeconds(30)));

        perCache.put(CacheKeys.COUPON_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.COUPON_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY, base.entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.ORDER_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.ORDERS_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.ORDERS_FILTER_STATUS_KEY_PREFIX, base.entryTtl(Duration.ofSeconds(30)));

        perCache.put(CacheKeys.PROFILE_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheKeys.PROFILES_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConf)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}
