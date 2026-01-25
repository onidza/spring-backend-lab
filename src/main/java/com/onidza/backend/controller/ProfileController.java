package com.onidza.backend.controller;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import com.onidza.backend.service.CacheMode;
import com.onidza.backend.service.profile.ManualProfileServiceImpl;
import com.onidza.backend.service.profile.ProfileService;
import com.onidza.backend.service.profile.ProfileServiceImpl;
import com.onidza.backend.service.profile.SpringCachingProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileServiceImpl profileServiceImpl;
    private final ManualProfileServiceImpl manualProfileService;
    private final SpringCachingProfileServiceImpl springCachingProfileService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> getProfile(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getProfile with id: {}", id);

        ProfileService service = resolveProfileService(cacheMode);
        ProfileDTO profileDTO = service.getProfileById(id);
        return ResponseEntity.ok(profileDTO);
    }

    @GetMapping("/profiles")
    public ResponseEntity<ProfilesPageDTO> getProfilesPage(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Called getProfilesPage");

        ProfileService service = resolveProfileService(cacheMode);
        ProfilesPageDTO profiles = service.getProfilesPage(page, size);
        return ResponseEntity.ok(profiles);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> updateProfileToClient(
            @PathVariable Long id,
            @Valid @RequestBody ProfileDTO profileDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called updateProfileToClient with id: {}", id);

        ProfileService service = resolveProfileService(cacheMode);
        ProfileDTO profile = service.updateProfile(id, profileDTO);
        return ResponseEntity.ok(profile);
    }

    private ProfileService resolveProfileService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> profileServiceImpl;
            case MANUAL -> manualProfileService;
            case SPRING -> springCachingProfileService;
        };
    }
}
