package com.onidza.backend.model.mapper;

import com.onidza.backend.model.dto.ProfileDTO;
import com.onidza.backend.model.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileMapper {

    public ProfileDTO toDTO(Profile profile) {
        if (profile == null) return null;

        Long clientId = profile.getClient() == null ? null : profile.getClient().getId();
        return new ProfileDTO(
                profile.getId(),
                profile.getAddress(),
                profile.getPhone(),
                clientId
        );
    }

    public Profile toEntity(ProfileDTO profileDTO) {
        if (profileDTO == null) return null;
        return new Profile(
                profileDTO.address(),
                profileDTO.phone()
        );
    }
}
