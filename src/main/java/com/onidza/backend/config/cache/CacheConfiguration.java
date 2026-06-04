package com.onidza.backend.config.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.cache.spring.CacheSpringKeys;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@EnableCaching
@Configuration
public class CacheConfiguration {

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

    /*
 Дефолтную JDK-сериализацию в Redis-кэше обычно не любят потому что:
1. Нужен implements Serializable у DTO.
2. Формат нечитаемый в Redis.
3. Формат жёстко завязан на Java.
4. Плохо переживает изменения классов.
5. Неудобно дебажить и смотреть данные руками.

=> ниже переопределенный конфиг для spring cache
*/

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
        perCache.put(CacheSpringKeys.CLIENT_KEY_PREFIX, base
                .serializeValuesWith(json.apply(ClientDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.CLIENTS_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(ClientsPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.COUPON_KEY_PREFIX, base
                .serializeValuesWith(json.apply(CouponDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.COUPON_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(CouponPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX, base
                .serializeValuesWith(json.apply(CouponPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.ORDER_KEY_PREFIX, base
                .serializeValuesWith(json.apply(OrderDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.ORDERS_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.ORDERS_FILTER_STATUS_KEY_PREFIX, base
                .serializeValuesWith(json.apply(OrdersPageDTO.class))
                .entryTtl(Duration.ofSeconds(30)));

        perCache.put(CacheSpringKeys.PROFILE_KEY_PREFIX, base
                .serializeValuesWith(json.apply(ProfileDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        perCache.put(CacheSpringKeys.PROFILES_PAGE_PREFIX, base
                .serializeValuesWith(json.apply(ProfilesPageDTO.class))
                .entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConf)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
//                .transactionAware()

                /*
Если используешь @CachePut/@CacheEvict на transactional-методах
→ включи .transactionAware()

Если сложная инвалидация через события
→ оставь @TransactionalEventListener(AFTER_COMMIT)

Если используешь оба подхода
→ это нормально, но нужно явно понимать: аннотации чистят простые ключи, события чистят связанные/страничные кэши.*/
                .build();
    }
}
