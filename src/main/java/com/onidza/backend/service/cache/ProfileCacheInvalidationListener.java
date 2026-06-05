package com.onidza.backend.service.cache;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.dto.client.events.ActionPart;
import com.onidza.backend.model.dto.client.events.client.ClientAddEvent;
import com.onidza.backend.model.dto.client.events.client.ClientDeletedEvent;
import com.onidza.backend.model.dto.client.events.client.ClientUpdateEvent;
import com.onidza.backend.model.dto.client.events.profile.ProfileUpdateEvent;
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
public class ProfileCacheInvalidationListener {

    private final CacheVersionService versionService;
    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientUpdated(ProfileUpdateEvent e) {
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
        versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

        Cache clientCache = cacheManager.getCache(CacheKeys.CLIENTS_PAGE_PREFIX);

        if (clientCache != null)
            clientCache.evict(e.clientId());
    }
}
