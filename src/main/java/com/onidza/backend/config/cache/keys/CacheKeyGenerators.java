package com.onidza.backend.config.cache.keys;

import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.enums.OrderStatus;
import com.onidza.backend.service.cache.CacheVersionService;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheKeyGenerators {

    private static final String PAGE_KEY_FORMAT = "ver=%d:p=%s:s=%s";
    private static final String BY_CLIENT_PAGE_KEY_FORMAT = "clientId=%s:ver=%d:p=%s:s=%s";
    private static final String FILTER_STATUS_KEY_FORMAT = "ver=%d:status=%s:p=%d:s=%d";

    @Bean
    public KeyGenerator clientPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY
            );

            return PAGE_KEY_FORMAT.formatted(ver, params[0], params[1]);
        };
    }

    @Bean
    public KeyGenerator couponPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.COUPON_PAGE_VER_KEY
            );

            return PAGE_KEY_FORMAT.formatted(ver, params[0], params[1]);
        };
    }

    @Bean
    public KeyGenerator couponPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            Long clientId = (Long) params[0];

            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_FORMATTED_KEY
                            .formatted(clientId)
            );

            return BY_CLIENT_PAGE_KEY_FORMAT.formatted(
                    clientId,
                    ver,
                    params[1],
                    params[2]
            );
        };
    }

    @Bean
    public KeyGenerator orderPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY
            );

            return PAGE_KEY_FORMAT.formatted(ver, params[0], params[1]);
        };
    }

    @Bean
    public KeyGenerator orderPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            Long clientId = (Long) params[0];

            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_FORMATTED_KEY
                            .formatted(clientId)
            );

            return BY_CLIENT_PAGE_KEY_FORMAT.formatted(
                    clientId,
                    ver,
                    params[1],
                    params[2]
            );
        };
    }

    @Bean
    public KeyGenerator filterStatusKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            OrderFilterDTO filter = (OrderFilterDTO) params[0];
            OrderStatus status = filter.status();
            int page = (int) params[1];
            int size = (int) params[2];

            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER
            );

            return FILTER_STATUS_KEY_FORMAT.formatted(
                    ver,
                    status,
                    page,
                    size
            );
        };
    }

    @Bean
    public KeyGenerator profilePageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(
                    CacheVersionKeys.PROFILES_PAGE_VER_KEY
            );

            return PAGE_KEY_FORMAT.formatted(ver, params[0], params[1]);
        };
    }
}
