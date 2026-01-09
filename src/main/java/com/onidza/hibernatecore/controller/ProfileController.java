package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.service.CacheMode;
import com.onidza.hibernatecore.service.profile.ManualProfileServiceImpl;
import com.onidza.hibernatecore.service.profile.ProfileService;
import com.onidza.hibernatecore.service.profile.ProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileServiceImpl profileServiceImpl;
    private final ManualProfileServiceImpl manualProfileService;

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
    public ResponseEntity<List<ProfileDTO>> getAllProfiles(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllProfiles");

        ProfileService service = resolveProfileService(cacheMode);
        List<ProfileDTO> profiles = service.getAllProfiles();
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
            case SPRING -> throw new UnsupportedOperationException("Have no such a service");
        };
    }
}
