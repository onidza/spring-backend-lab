package com.onidza.backend.service.cache.listeners;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.events.coupon.CouponAddEvent;
import com.onidza.backend.model.events.coupon.CouponDeleteEvent;
import com.onidza.backend.model.events.coupon.CouponUpdateEvent;
import com.onidza.backend.service.cache.CacheVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponCacheInvalidationListener {

    private final CacheVersionService versionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCouponAdded(CouponAddEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_FORMATTED_KEY
                .formatted(e.clientId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCouponUpdated(CouponUpdateEvent e) {
        e.clientIds().forEach(id -> {
            versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, id);
            versionService.bumpVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_FORMATTED_KEY
                    .formatted(id));
        });
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCouponDeleted(CouponDeleteEvent e) {
        e.clientIds().forEach(id -> {
            versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, id);
            versionService.bumpVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_FORMATTED_KEY
                    .formatted(id));
        });

        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.evictCache(CacheKeys.COUPON_KEY_PREFIX, e.couponId());
        versionService.bumpVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);
    }
}
