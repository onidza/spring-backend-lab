package com.onidza.backend.service;

import com.onidza.backend.cache.config.CacheVersionService;
import com.onidza.backend.cache.config.manual.CacheManualVersionKeys;
import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.cache.config.spring.CacheSpringVersionKeys;
import com.onidza.backend.model.dto.client.events.ClientAddEvent;
import com.onidza.backend.model.dto.client.ClientActionPart;
import com.onidza.backend.model.dto.client.events.ClientDeletedEvent;
import com.onidza.backend.model.dto.client.events.ClientUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheInvalidationListener {

    private final CacheVersionService versionService;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientAdded(ClientAddEvent e) {
        versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

        log.info("Key {}, {} was incremented.",
                CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY
        );

        if (e.parts().contains(ClientActionPart.COUPONS)) {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            log.info("Keys: {} was incremented.", CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
        }

        if (e.parts().contains(ClientActionPart.ORDERS)) {
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            log.info("Keys: {}, {} was incremented.",
                    CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientUpdated(ClientUpdateEvent e) {
        versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);
        redisTemplate.delete(CacheSpringKeys.PROFILE_KEY_PREFIX + e.profileId()); //todo swap to cacheManager
        versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

        log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY,
                CacheSpringKeys.PROFILE_KEY_PREFIX + e.profileId()
        );

        if (e.parts().contains(ClientActionPart.COUPONS)) {
            e.couponIdsToEvict().forEach(couponId ->
                    redisTemplate.delete(CacheSpringKeys.COUPON_KEY_PREFIX + couponId)); //todo swap to cacheManager

            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringKeys.COUPON_KEY_PREFIX
            );
        }

        if (e.parts().contains(ClientActionPart.ORDERS)) {
            e.orderIdsToEvict().forEach(orderId ->
                    redisTemplate.delete(CacheSpringKeys.ORDER_KEY_PREFIX + orderId)); //todo swap to cacheManager

            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheSpringKeys.ORDER_KEY_PREFIX
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientDeleted(ClientDeletedEvent e) {
        versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);
        redisTemplate.delete(CacheSpringKeys.PROFILE_KEY_PREFIX + e.profileId()); //todo swap to cacheManager

        log.info("Key {} was incremented. Key {} was invalidated.",
                CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                CacheSpringKeys.PROFILE_KEY_PREFIX
        );

        if (e.parts().contains(ClientActionPart.COUPONS)) {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            log.info("Key {} was incremented.", CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
        }

        if (e.parts().contains(ClientActionPart.ORDERS)) {
            e.orderIdsToEvict().forEach(orderId ->
                    redisTemplate.delete(CacheSpringKeys.ORDER_KEY_PREFIX + orderId)); //todo swap to cacheManager

            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheSpringKeys.ORDER_KEY_PREFIX
            );
        }
    }
}
