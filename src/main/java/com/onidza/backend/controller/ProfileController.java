package com.onidza.backend.controller;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import com.onidza.backend.service.profile.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Validated
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> getProfile(
            @PathVariable @Positive Long id
    ) {
        log.info("ProfileService called getProfile with id = {}", id);
        ProfileDTO profileDTO = profileService.getProfileById(id);

        return ResponseEntity.ok(profileDTO);
    }

    @GetMapping("/profiles")
    public ResponseEntity<ProfilesPageDTO> getProfilesPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        log.info("ProfileService called getProfilesPage, page = {}, size = {}", page, size);
        ProfilesPageDTO profiles = profileService.getProfilesPage(page, size);

        return ResponseEntity.ok(profiles);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> updateProfileByClientId(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProfileDTO profileDTO
    ) {
        log.info("ProfileService called updateProfileByClientId with id = {}", id);
        ProfileDTO profile = profileService.updateProfileByClientId(id, profileDTO);

        return ResponseEntity.ok(profile);
    }
}
