package com.onidza.backend.service.cache.listeners;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.events.ActionPart;
import com.onidza.backend.model.events.client.ClientAddEvent;
import com.onidza.backend.model.events.client.ClientDeletedEvent;
import com.onidza.backend.model.events.client.ClientUpdateEvent;
import com.onidza.backend.service.cache.CacheVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ClientCacheInvalidationListener {

    private final CacheVersionService versionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientAdded(ClientAddEvent e) {
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

        if (e.parts().contains(ActionPart.ORDERS)) {
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
        }

        if (e.parts().contains(ActionPart.COUPONS))
            versionService.bumpVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientUpdated(ClientUpdateEvent e) {
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.evictCache(CacheKeys.PROFILE_KEY_PREFIX, e.profileId());
        versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientDeleted(ClientDeletedEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.evictCache(CacheKeys.PROFILE_KEY_PREFIX, e.profileId());
        versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

        if (e.parts().contains(ActionPart.ORDERS)) {
            e.orderIdsToEvict().forEach(id -> versionService.evictCache(
                    CacheKeys.ORDER_KEY_PREFIX, id
            ));

            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
        }

        if (e.parts().contains(ActionPart.COUPONS)) {
            e.couponIdsToEvict().forEach(id -> versionService.evictCache(
                    CacheKeys.COUPON_KEY_PREFIX, id)
            );

            versionService.bumpVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
        }
    }
}
