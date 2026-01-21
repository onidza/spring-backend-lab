package com.onidza.backend.config.manual;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheKeys {

    public static final String CLIENT_KEY_PREFIX = "client:id:";
    public static final String CLIENTS_PAGE_VER_KEY = "clients:all:ver=";

    public static final String COUPON_KEY_PREFIX = "coupon:id:";
    public static final String COUPON_PAGE_VER_KEY = "coupons:all:ver=";
    public static final String COUPONS_PAGE_BY_CLIENT_ID_VER_KEY = "coupons:byClientId:";

    public static final String ORDER_KEY_PREFIX = "order:id:";
    public static final String PAGE_ORDERS_VER_KEY = "orders:all:ver=";
    public static final String PAGE_ORDERS_BY_CLIENT_ID_VER_KEY = "orders:byClientId:";
    public static final String ORDERS_FILTER_STATUS_KEY_PREFIX = "orders:filter:status:";

    public static final String PROFILE_KEY_PREFIX = "profile:";
    public static final String PAGE_PROFILES_VER_KEY = "profile:all:ver=";
}
