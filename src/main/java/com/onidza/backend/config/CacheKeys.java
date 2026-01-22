package com.onidza.backend.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class CacheKeys {

    public static final String CLIENT_KEY_PREFIX = "client";
    public static final String CLIENTS_PAGE_VER_KEY = "clientPage";

    public static final String COUPON_KEY_PREFIX = "coupon";
    public static final String COUPON_PAGE_VER_KEY = "couponsPage";
    public static final String COUPONS_PAGE_BY_CLIENT_ID_VER_KEY = "couponsPage:byClientId";

    public static final String ORDER_KEY_PREFIX = "order";
    public static final String ORDERS_PAGE_VER_KEY = "ordersPage";
    public static final String ORDERS_PAGE_BY_CLIENT_ID_VER_KEY = "ordersPage:byClientId";
    public static final String ORDERS_FILTER_STATUS_KEY_PREFIX = "orders:filter:status";

    public static final String PROFILE_KEY_PREFIX = "profile";
    public static final String PROFILES_PAGE_VER_KEY = "profilePage";

}
