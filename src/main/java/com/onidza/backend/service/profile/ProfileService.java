package com.onidza.backend.service.profile;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;

public interface ProfileService {

    ProfileDTO getProfileById(Long id);

    ProfilesPageDTO getProfilesPage(int page, int size);

    ProfileDTO updateProfileByClientId(Long id, ProfileDTO profileDTO);
}
