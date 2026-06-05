package com.onidza.backend.config.cache.keys;

import com.onidza.backend.service.cache.CacheVersionService;
import com.onidza.backend.model.dto.enums.OrderStatus;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("java:S2094")
public class CacheKeyGenerators {

    private static final String VER_BEGIN = "ver=";
    private static final String VER_CONTINUE = ":ver=";
    private static final String PAGE_CONTINUE = ":p=";
    private static final String SIZE_CONTINUE = ":s=";

    @Bean
    public KeyGenerator clientPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

            return VER_BEGIN + ver + PAGE_CONTINUE + params[0] + SIZE_CONTINUE + params[1];
        };
    }

    @Bean
    public KeyGenerator couponPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.COUPON_PAGE_VER_KEY);

            return VER_BEGIN + ver + PAGE_CONTINUE + params[0] + SIZE_CONTINUE + params[1];
        };
    }

    @Bean
    public KeyGenerator couponPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            return params[0] + VER_CONTINUE + ver + PAGE_CONTINUE + params[1] + SIZE_CONTINUE + params[2];
        };
    }

    @Bean
    public KeyGenerator orderPageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);

            return VER_BEGIN + ver + PAGE_CONTINUE + params[0] + SIZE_CONTINUE + params[1];
        };
    }

    @Bean
    public KeyGenerator orderPageByClientIdKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);

            return params[0] + VER_CONTINUE + ver + PAGE_CONTINUE + params[1] + SIZE_CONTINUE + params[2];
        };
    }

    @Bean
    public KeyGenerator filterStatusKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            OrderFilterDTO filter = (OrderFilterDTO) params[0];
            OrderStatus status = filter.status();
            long ver = versionService.getKeyVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            return status + VER_CONTINUE + ver;
        };
    }

    @Bean
    public KeyGenerator profilePageKeyGen(CacheVersionService versionService) {
        return (target, method, params) -> {
            long ver = versionService.getKeyVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

            return ver + PAGE_CONTINUE + params[0] + SIZE_CONTINUE + params[1];
        };
    }
}
