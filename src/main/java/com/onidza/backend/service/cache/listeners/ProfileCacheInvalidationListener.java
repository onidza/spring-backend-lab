package com.onidza.backend.service.cache.listeners;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.events.profile.ProfileUpdateEvent;
import com.onidza.backend.service.cache.CacheVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileCacheInvalidationListener {

    private final CacheVersionService versionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientUpdated(ProfileUpdateEvent e) {
        versionService.evictCache(CacheKeys.CLIENT_KEY_PREFIX, e.clientId());
        versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

        versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);
    }
}
