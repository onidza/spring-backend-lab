package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ProfileDTO;

import java.util.List;

public interface ProfileService {

    ProfileDTO getProfileById(Long id);

    List<ProfileDTO> getAllProfiles();

    ProfileDTO updateProfile(Long id, ProfileDTO profileDTO);
}
