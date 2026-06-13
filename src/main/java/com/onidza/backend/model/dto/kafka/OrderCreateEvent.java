package com.onidza.backend.model.dto.kafka;

import com.onidza.backend.model.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderCreateEvent(
        Long clientId,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
