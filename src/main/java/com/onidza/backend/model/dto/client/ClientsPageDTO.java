package com.onidza.backend.model.dto.client;

import java.util.List;

public record ClientsPageDTO (
        List<ClientDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
