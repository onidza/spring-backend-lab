package com.onidza.backend.model.dto.order;

import com.onidza.backend.model.dto.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OrderCreateEvent(
    UUID id,
    Long clientId,
    LocalDateTime orderDate,
    BigDecimal totalAmount,
    OrderStatus status
) {}
