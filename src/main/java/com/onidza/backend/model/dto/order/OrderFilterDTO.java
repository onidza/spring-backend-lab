package com.onidza.backend.model.dto.order;

import com.onidza.backend.model.entity.Order;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Generated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link Order}
 * */

@Generated
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
