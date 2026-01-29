package com.onidza.backend.cache.config.spring;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class CacheSpringKeys {

    public static final String CLIENT_KEY_PREFIX = "client:id";
    public static final String CLIENTS_PAGE_PREFIX = "clientsPage";

    public static final String COUPON_KEY_PREFIX = "coupon";
    public static final String COUPON_PAGE_PREFIX = "couponsPage";
    public static final String COUPONS_PAGE_BY_CLIENT_ID_PREFIX = "couponsPageByClientId";

    public static final String ORDER_KEY_PREFIX = "order:id";
    public static final String ORDERS_PAGE_PREFIX = "ordersPage";
    public static final String ORDERS_PAGE_BY_CLIENT_ID_PREFIX = "ordersPageByClientId";
    public static final String ORDERS_FILTER_STATUS_KEY_PREFIX = "orders:filter:status";

    public static final String PROFILE_KEY_PREFIX = "profile:id";
    public static final String PROFILES_PAGE_PREFIX = "profilePage";

}
