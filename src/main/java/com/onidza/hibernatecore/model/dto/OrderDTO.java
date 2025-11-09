package com.onidza.hibernatecore.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDTO(
        Long id,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        String status,
        Long clientId
) {}
