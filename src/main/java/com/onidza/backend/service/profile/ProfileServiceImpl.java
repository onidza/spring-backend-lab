package com.onidza.backend.service.profile;

import com.onidza.backend.model.dto.ProfileDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    public ProfileDTO getProfileById(Long id) {
        log.info("Called getProfileById with id: {}", id);

        return mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
    }

    public List<ProfileDTO> getAllProfiles() {
        log.info("Called getAllProfiles");

        return profileRepository.findAll()
                .stream()
                .map(mapperService::profileToDTO)
                .toList();
    }

    @Transactional
    public ProfileDTO updateProfile(Long id, ProfileDTO profileDTO) {
        log.info("Called updateProfile with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Profile profile = client.getProfile();

        if (profile != null) {
            profile.setAddress(profileDTO.address());
            profile.setPhone(profileDTO.phone());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client hasn't a profile");
        }

        return mapperService.profileToDTO(profile);
    }
}
