package com.onidza.hibernatecore.model.dto;

import com.onidza.hibernatecore.model.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.Generated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link Coupon}
 * */

@Generated
public record CouponDTO(
        Long id,

        @NotEmpty(message = "Code can't be empty")
        @Size(min = 10, max = 15, message = "Coupon code must have length from 10 to 15 symbols")
        String code,

        @Min(value = 5, message = "Discount must be at least 5%")
        @Max(value = 40, message = "Discount must be at most 40%")
        float discount,

        @NotNull(message = "ExpirationDate can't be empty")
        LocalDateTime expirationDate,

        List<Long> clientsId
) {}
