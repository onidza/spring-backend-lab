package com.onidza.backend.config.cache.manual;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public record CacheTtlProps(

    Duration clientById,
    Duration getClientsPage,

    Duration getCouponById,
    Duration getCouponsPage,
    Duration getCouponsPageByClientId,

    Duration getOrderById,
    Duration getOrdersPage,
    Duration getOrdersPageByClientId,
    Duration getOrdersByFilters,

    Duration getProfileById,
    Duration getProfilesPage
) {}
