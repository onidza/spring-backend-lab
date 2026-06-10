package com.onidza.backend.service.profile;

import com.onidza.backend.model.dto.client.events.profile.ProfileUpdateEvent;
import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.PROFILE_KEY_PREFIX,
            key = "#id"
    )
    public ProfileDTO getProfileById(Long id) {
        log.info("ProfileServiceImpl getProfileById with id = {}", id);

        return mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.PROFILES_PAGE_PREFIX,
            keyGenerator = "profilePageKeyGen"
    )
    public ProfilesPageDTO getProfilesPage(int page, int size) {
        log.info("ProfileServiceImpl getProfilesPage, page = {}, size = {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id"));

        Page<ProfileDTO> result =  profileRepository.findAllProfiles(pageable)
                .map(mapperService::profileToDTO);

        return new ProfilesPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    @CachePut(
            cacheNames = CacheKeys.PROFILE_KEY_PREFIX,
            key = "#result.id()"
    )
    public ProfileDTO updateProfileByClientId(Long id, ProfileDTO profileDTO) {
        log.info("ProfileServiceImpl called updateProfileByClientId with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Profile profile = client.getProfile();

        if (profile != null)
            profile.updateInfo(profileDTO.address(), profileDTO.phone());
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client hasn't a profile");

        publisher.publishEvent(new ProfileUpdateEvent(id));

        return mapperService.profileToDTO(profile);
    }
}
