package com.onidza.backend.cache.config.manual;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheManualVersionKeys {

    public static final String CLIENTS_PAGE_VER_KEY = "clientPage:ver";

    public static final String COUPON_PAGE_VER_KEY = "couponsPage:ver";
    public static final String COUPONS_PAGE_BY_CLIENT_ID_VER_KEY = "couponsPage:byClientId:ver";

    public static final String ORDERS_PAGE_VER_KEY = "ordersPage:ver";
    public static final String ORDERS_PAGE_BY_CLIENT_ID_VER_KEY = "ordersPage:byClientId:ver";
    public static final String ORDERS_FILTER_STATUS_KEY_VER = "orders:filter:status:ver";

    public static final String PROFILES_PAGE_VER_KEY = "profilePage:ver";
}
