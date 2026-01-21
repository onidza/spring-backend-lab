package com.onidza.backend.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("java:S2094")
public class CacheKeyGenerators {

    @Bean
    public KeyGenerator clientPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            int page = (int) params[0];
            int size = (int) params[1];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheKeys.CLIENTS_PAGE_VER_KEY);

            return "ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }

    @Bean
    public KeyGenerator couponPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            int page = (int) params[0];
            int size = (int) params[1];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheKeys.COUPON_PAGE_VER_KEY);

            return "ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }

    @Bean
    public KeyGenerator couponPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long clientId = (long) params[0];
            int page = (int) params[1];
            int size = (int) params[2];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            return clientId + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }
}
