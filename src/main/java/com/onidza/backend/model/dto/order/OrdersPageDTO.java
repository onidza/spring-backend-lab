package com.onidza.backend.model.dto.order;

import java.util.List;

public record OrdersPageDTO(
        List<OrderDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
