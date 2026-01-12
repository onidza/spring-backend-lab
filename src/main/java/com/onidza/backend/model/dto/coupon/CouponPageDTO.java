package com.onidza.backend.model.dto.coupon;

import java.util.List;

public record CouponPageDTO(
        List<CouponDTO> items,
        int page,
        int size,
        boolean hasNext
) {}
