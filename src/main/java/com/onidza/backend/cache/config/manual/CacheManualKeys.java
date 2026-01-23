package com.onidza.backend.cache.config.manual;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheManualKeys {

    public static final String CLIENT_KEY_PREFIX = "client:id:";
    public static final String CLIENTS_PAGE_PREFIX = "clients:all:ver=";

    public static final String COUPON_KEY_PREFIX = "coupon:id:";
    public static final String COUPON_PAGE_PREFIX = "coupons:all:ver=";
    public static final String COUPONS_PAGE_BY_CLIENT_ID_PREFIX = "coupons:byClientId:";

    public static final String ORDER_KEY_PREFIX = "order:id:";
    public static final String ORDERS_PAGE_PREFIX = "orders:all:ver=";
    public static final String ORDERS_PAGE_BY_CLIENT_ID_PREFIX = "orders:byClientId:";
    public static final String ORDERS_FILTER_STATUS_PREFIX = "orders:filter:status:";

    public static final String PROFILE_KEY_PREFIX = "profile:id:";
    public static final String PROFILES_PAGE_PREFIX = "profile:all:ver=";
}
