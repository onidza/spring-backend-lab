package com.onidza.backend.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.CacheVersionKeys;
import com.onidza.backend.config.CacheTtlProps;
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
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ManualProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final CacheTtlProps ttlProps;
    private final CacheVersionService versionService;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(Long id) {
        log.info("Called getProfileById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(CacheVersionKeys.PROFILE_KEY_PREFIX + id);
        if (objFromCache != null) {
            log.info("Returned profile from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, ProfileDTO.class);
        }

        ProfileDTO existing = mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));

        redisTemplate.opsForValue().set(CacheVersionKeys.PROFILE_KEY_PREFIX + id, existing, ttlProps.getProfileById());
        log.info("getProfileById was cached...");

        log.info("Returned profile from db with id: {}", id);
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfilesPageDTO getProfilesPage(int page, int size) {
        log.info("Called getAllProfiles");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        long ver = versionService.getKeyVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);
        String key = CacheVersionKeys.PROFILES_PAGE_VER_KEY + ver + ":p=" + safePage + ":s=" + safeSize;

        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            ProfilesPageDTO cached = objectMapper.convertValue(objFromCache, ProfilesPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Slice<ProfileDTO> result = profileRepository.findBy(pageable)
                .map(mapperService::profileToDTO);

        ProfilesPageDTO response = new ProfilesPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, ttlProps.getProfilesPage());
        log.info("getProfilesPage was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(Long clientId, ProfileDTO profileDTO) {
        log.info("Called updateProfile with clientId: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Profile profile = client.getProfile();

        if (profile != null) {
            profile.setAddress(profileDTO.address());
            profile.setPhone(profileDTO.phone());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client hasn't a profile");
        }

        Long profileId = profile.getId();

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheVersionKeys.PROFILE_KEY_PREFIX + profileId);
            versionService.bumpVersion(CacheVersionKeys.PROFILES_PAGE_VER_KEY);

            redisTemplate.delete(CacheVersionKeys.CLIENT_KEY_PREFIX + clientId);
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {} was incremented. Keys {}, {} was invalidated.",
                    CacheVersionKeys.PROFILES_PAGE_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,

                    CacheVersionKeys.PROFILE_KEY_PREFIX + profileId,
                    CacheVersionKeys.CLIENT_KEY_PREFIX + clientId
            );
        });

        return mapperService.profileToDTO(profile);
    }
}
