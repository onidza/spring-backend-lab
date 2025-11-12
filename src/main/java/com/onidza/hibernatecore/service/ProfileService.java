package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    public ProfileDTO getProfileById(Long id) {
        return mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
    }

    public List<ProfileDTO> getAllProfiles() {
        return profileRepository
                .findAll()
                .stream()
                .map(mapperService::profileToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileDTO updateProfile(Long id, ProfileDTO profileDTO) {
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
