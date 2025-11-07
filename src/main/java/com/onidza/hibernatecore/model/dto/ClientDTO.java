package com.onidza.hibernatecore.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClientDTO(
        Long id,
        String name,
        String email,
        LocalDateTime registrationDate,
        ProfileDTO profile,
        List<OrderDTO> orders,
        List<CouponDTO> coupons
) {}
