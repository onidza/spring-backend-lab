package com.onidza.backend.model.events.coupon;

import java.util.Set;

public record CouponUpdateEvent(
    Set<Long> clientIds,
    Long couponId
) {
}
