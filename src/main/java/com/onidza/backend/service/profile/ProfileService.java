package com.onidza.backend.service.profile;

import com.onidza.backend.model.dto.ProfileDTO;

import java.util.List;

public interface ProfileService {

    ProfileDTO getProfileById(Long id);

    List<ProfileDTO> getAllProfiles();

    ProfileDTO updateProfile(Long id, ProfileDTO profileDTO);
}
