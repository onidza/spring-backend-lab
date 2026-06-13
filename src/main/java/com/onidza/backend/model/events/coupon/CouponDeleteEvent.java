package com.onidza.backend.model.events.coupon;

import java.util.Set;

public record CouponDeleteEvent(
        Set<Long> clientIds,
        Long couponId
) {
}
