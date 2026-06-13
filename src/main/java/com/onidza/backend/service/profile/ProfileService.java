package com.onidza.backend.service.profile;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;

public interface ProfileService {

    ProfileDTO getProfile(Long id);

    ProfilesPageDTO getProfilesPage(int page, int size);

    ProfileDTO updateProfile(Long id, ProfileDTO profileDTO);
}
