package com.onidza.backend.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.ProfileDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.ProfileRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ManualProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private static final String PROFILE_KEY_PREFIX = "profile:";
    private static final Duration PROFILE_TTL = Duration.ofMinutes(10);

    private static final String ALL_PROFILES_KEY = "profile:all:v1";
    private static final Duration ALL_PROFILES_TTL = Duration.ofMinutes(10);

    private static final String CLIENT_KEY_PREFIX = "client:";
    private static final String ALL_CLIENTS_KEY = "clients:all:v1";

    public ProfileDTO getProfileById(Long id) {
        log.info("Called getProfileById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(PROFILE_KEY_PREFIX + id);
        if (objFromCache != null) {
            log.info("Returned profile from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, ProfileDTO.class);
        }

        ProfileDTO existing = mapperService.profileToDTO(profileRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));

        redisTemplate.opsForValue().set(PROFILE_KEY_PREFIX + id, existing, PROFILE_TTL);
        log.info("getProfileById was cached...");

        log.info("Returned profile from db with id: {}", id);
        return existing;
    }

    public List<ProfileDTO> getAllProfiles() {
        log.info("Called getAllProfiles");

        Object objFromCache = redisTemplate.opsForValue().get(ALL_PROFILES_KEY);
        if (objFromCache instanceof List<?> raw) {
            List<ProfileDTO> dtoList = raw.stream()
                    .map(p -> objectMapper.convertValue(p, ProfileDTO.class))
                    .toList();

            log.info("Returned profiles from cache with size: {}", dtoList.size());
            return dtoList;
        }

        List<ProfileDTO> dtoList = profileRepository.findAll()
                .stream()
                .map(mapperService::profileToDTO)
                .toList();

        redisTemplate.opsForValue().set(ALL_PROFILES_KEY, dtoList, ALL_PROFILES_TTL);
        log.info("getAllProfiles was cached...");

        log.info("Returned profiles from db with size: {}", dtoList.size());
        return dtoList;

    }

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
            redisTemplate.delete(PROFILE_KEY_PREFIX + profileId);
            redisTemplate.delete(ALL_PROFILES_KEY);
            log.info("Updated profile was invalidated in cache with key={}", PROFILE_KEY_PREFIX + profileId);
            log.info("Updated profile was invalidated in cache with key={}", ALL_PROFILES_KEY);

            redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
            redisTemplate.delete(ALL_CLIENTS_KEY);
            log.info("Updated profile in getClientById was invalidated with key={}", CLIENT_KEY_PREFIX + clientId);
            log.info("Updated profile in getAllClients was invalidated with key={}", ALL_CLIENTS_KEY);
        });

        return mapperService.profileToDTO(profile);
    }
}
