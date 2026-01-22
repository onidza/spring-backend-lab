package com.onidza.backend.config;

import com.onidza.backend.model.OrderStatus;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
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

            long ver = versionService.getKeyVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

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

            long ver = versionService.getKeyVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);

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

            long ver = versionService.getKeyVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            return clientId + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }

    @Bean
    public KeyGenerator orderPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            int page = (int) params[0];
            int size = (int) params[1];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);

            return "ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }

    @Bean
    public KeyGenerator orderPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long clientId = (long) params[0];
            int page = (int) params[1];
            int size = (int) params[2];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);

            return clientId + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }

    @Bean
    public KeyGenerator filterStatusKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            OrderFilterDTO filter = (OrderFilterDTO) params[0];
            OrderStatus status = filter.status();

            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX);

            return status + ":ver=" + ver;
        };
    }

    @Bean
    public KeyGenerator profilePageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            int page = (int) params[0];
            int size = (int) params[1];

            int safeSize = Math.min(Math.max(size, 1), 20);
            int safePage = Math.max(page, 0);

            long ver = versionService.getKeyVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

            return ver + ":p=" + safePage + ":s=" + safeSize;
        };
    }
}
