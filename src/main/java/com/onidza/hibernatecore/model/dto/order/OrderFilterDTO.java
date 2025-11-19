package com.onidza.hibernatecore.model.dto.order;

import com.onidza.hibernatecore.model.OrderStatus;
import com.onidza.hibernatecore.model.entity.Order;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link Order}
 * */

public record OrderFilterDTO(
        OrderStatus status,

        @PastOrPresent(message = "fromDate must have before now or now")
        LocalDateTime fromDate,

        @PastOrPresent(message = "toDate must have before now or now")
        LocalDateTime toDate,

        @PositiveOrZero
        BigDecimal minAmount,

        @PositiveOrZero
        BigDecimal maxAmount
)
{}
