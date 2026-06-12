package com.onidza.backend.config.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@EnableCaching
@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        Function<Class<?>, RedisSerializationContext.SerializationPair<Object>> json =
                clazz -> {
                    Jackson2JsonRedisSerializer<?> ser = new Jackson2JsonRedisSerializer<>(objectMapper, clazz);

                    @SuppressWarnings("unchecked")
                    RedisSerializationContext.SerializationPair<Object> pair =
                            (RedisSerializationContext.SerializationPair<Object>)
                                    RedisSerializationContext.SerializationPair.fromSerializer((RedisSerializer<?>) ser);

                    return pair;
                };

        var defaultConf = base
                .serializeValuesWith(json.apply(Object.class))
                .entryTtl(Duration.ofMinutes(1));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheKeys.CLIENT_KEY_PREFIX, base
                .serializeValuesWith(json.apply(ClientDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.CLIENTS_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(ClientsPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.COUPON_KEY_PREFIX, base
                .serializeValuesWith(json.apply(CouponDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.COUPON_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(CouponPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX, base
                .serializeValuesWith(json.apply(CouponPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.ORDER_KEY_PREFIX, base
                .serializeValuesWith(json.apply(OrderDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.ORDERS_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.ORDERS_FILTER_STATUS_KEY_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofSeconds(30)));

        perCache.put(CacheKeys.PROFILE_KEY_PREFIX, base
                .serializeValuesWith(json.apply(ProfileDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheKeys.PROFILES_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(ProfilesPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConf)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}
