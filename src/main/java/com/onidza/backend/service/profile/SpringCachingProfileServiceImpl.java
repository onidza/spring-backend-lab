package com.onidza.backend.service.profile;

import com.onidza.backend.config.CacheKeys;
import com.onidza.backend.config.CacheVersionKeys;
import com.onidza.backend.config.CacheVersionService;
import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.ProfileRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringCachingProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final CacheVersionService versionService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.PROFILE_KEY_PREFIX,
            key = "'id:' + #id",
            condition = "#id > 0"
    )
    public ProfileDTO getProfileById(Long id) {
        log.info("Called getProfileById with id: {}", id);

        return mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.PROFILES_PAGE_VER_KEY,
            keyGenerator = "profilePageKeyGen"
    )
    public ProfilesPageDTO getProfilesPage(int page, int size) {
        log.info("Called getProfilesPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id"));

        Slice<ProfileDTO> result =  profileRepository.findBy(pageable)
                .map(mapperService::profileToDTO);

        return new ProfilesPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    @CachePut(
            cacheNames = CacheKeys.PROFILE_KEY_PREFIX,
            key = "'if:' + #id",
            condition = "#id > 0"
    )
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

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);
            log.info("Key {} was incremented.", CacheVersionKeys.PROFILES_PAGE_VER_KEY);
        });

        return mapperService.profileToDTO(profile);
    }
}
