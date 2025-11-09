package com.onidza.hibernatecore.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CouponDTO(
        Long id,
        String code,
        float discount,
        LocalDateTime expirationDate,
        List<Long> clientsId
) {}
