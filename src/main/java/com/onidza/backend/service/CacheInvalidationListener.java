package com.onidza.backend.service;

import com.onidza.backend.cache.config.CacheVersionService;
import com.onidza.backend.cache.config.manual.CacheManualVersionKeys;
import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.cache.config.spring.CacheSpringVersionKeys;
import com.onidza.backend.model.dto.client.ClientActionPart;
import com.onidza.backend.model.dto.client.events.ClientAddEvent;
import com.onidza.backend.model.dto.client.events.ClientDeletedEvent;
import com.onidza.backend.model.dto.client.events.ClientUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheInvalidationListener {

    private final CacheVersionService versionService;
    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientAdded(ClientAddEvent e) {
        versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

        log.info("Key {}, {} was incremented.",
                CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY,
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
        versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

        Cache profileCache = cacheManager.getCache(CacheSpringKeys.PROFILE_KEY_PREFIX);
        if (profileCache != null)
            profileCache.evict(e.profileId());

        log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY,
                CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY,
                CacheSpringKeys.PROFILE_KEY_PREFIX + e.profileId()
        );

        if (e.parts().contains(ClientActionPart.COUPONS)) {
            Cache couponCache = cacheManager.getCache(CacheSpringKeys.COUPON_KEY_PREFIX);
            if (couponCache != null)
                e.couponIdsToEvict().forEach(couponCache::evict);

            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringKeys.COUPON_KEY_PREFIX
            );
        }

        if (e.parts().contains(ClientActionPart.ORDERS)) {
            Cache orderCache = cacheManager.getCache(CacheSpringKeys.ORDER_KEY_PREFIX);
            if (orderCache != null)
                e.orderIdsToEvict().forEach(orderCache::evict);

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

        Cache profileCache = cacheManager.getCache(CacheSpringKeys.PROFILE_KEY_PREFIX);
        if (profileCache != null)
            profileCache.evict(e.profileId());

        log.info("Key {} was incremented. Key {} was invalidated.",
                CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY,
                CacheSpringKeys.PROFILE_KEY_PREFIX
        );

        if (e.parts().contains(ClientActionPart.COUPONS)) {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            log.info("Key {} was incremented.", CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
        }

        if (e.parts().contains(ClientActionPart.ORDERS)) {
            Cache orderCache = cacheManager.getCache(CacheSpringKeys.ORDER_KEY_PREFIX);
            if (orderCache != null)
                e.orderIdsToEvict().forEach(orderCache::evict);

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
