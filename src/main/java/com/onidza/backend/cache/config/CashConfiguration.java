package com.onidza.backend.cache.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.cache.config.spring.CacheSpringVersionKeys;
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
        perCache.put(CacheSpringKeys.CLIENT_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.CLIENTS_PAGE_PREFIX, base.entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.COUPON_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.COUPON_PAGE_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX, base.entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.ORDER_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.ORDERS_PAGE_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.ORDERS_FILTER_STATUS_KEY_PREFIX, base.entryTtl(Duration.ofSeconds(30)));

        perCache.put(CacheSpringKeys.PROFILE_KEY_PREFIX, base.entryTtl(Duration.ofMinutes(1)));
        perCache.put(CacheSpringKeys.PROFILES_PAGE_PREFIX, base.entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));

        perCache.put(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));
        perCache.put(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));

        perCache.put(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));
        perCache.put(CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));
        perCache.put(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER, base.entryTtl(Duration.ofMinutes(15)));

        perCache.put(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY, base.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConf)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}
