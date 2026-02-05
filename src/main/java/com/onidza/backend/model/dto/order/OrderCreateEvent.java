package com.onidza.backend.model.dto.order;

import com.onidza.backend.model.dto.enums.OrderStatus;
import lombok.Builder;
import lombok.Generated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Generated
@Builder
public record OrderCreateEvent(
        Long clientId,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        OrderStatus status
) {}
