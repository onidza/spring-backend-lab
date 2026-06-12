package com.onidza.backend.service.cache.listeners;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.events.order.OrderAddEvent;
import com.onidza.backend.model.events.order.OrderDeleteEvent;
import com.onidza.backend.model.events.order.OrderUpdateEvent;
import com.onidza.backend.service.cache.CacheVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCacheInvalidationListener {

    private final CacheVersionService versionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientAdded(OrderAddEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientUpdated(OrderUpdateEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientDeleted(OrderDeleteEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.evictCache(CacheKeys.ORDER_KEY_PREFIX, e.orderId());
        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
    }
}
