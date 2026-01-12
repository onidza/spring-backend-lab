package com.onidza.backend.model.dto.profile;

import java.util.List;

public record ProfilesPageDTO(
        List<ProfileDTO> items,
        int page,
        int size,
        boolean hasNext
) {}
