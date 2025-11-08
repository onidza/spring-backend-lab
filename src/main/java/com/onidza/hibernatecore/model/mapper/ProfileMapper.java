package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileMapper {

    private final ClientMapper clientMapper;

    public ProfileDTO toDTO(Profile profile) {
        if (profile == null) return null;
        return new ProfileDTO(
                profile.getId(),
                profile.getAddress(),
                profile.getPhone(),
                clientMapper.toDTO(profile.getClient())
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
