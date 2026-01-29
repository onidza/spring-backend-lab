package com.onidza.backend.model.dto.order;

import com.onidza.backend.model.entity.Order;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Generated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link Order}
 * */

@Generated
public record OrderDTO(
        Long id,

        @PastOrPresent(message = "Order date cannot be in the future")
        LocalDateTime orderDate,

        @Positive
        @DecimalMax(value = "1000000", message = "Too big total amount")
        BigDecimal totalAmount,

        @NotNull(message = "Status must be not empty")
        OrderStatus status,

        Long clientId
) {}
