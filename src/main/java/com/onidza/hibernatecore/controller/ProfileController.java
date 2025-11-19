package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.service.ProfileService;
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

    private final ProfileService profileService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> getProfile(@PathVariable Long id) {
        log.info("Called getProfile with id: {}", id);
        ProfileDTO profileDTO = profileService.getProfileById(id);
        return ResponseEntity.ok(profileDTO);
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<ProfileDTO>> getAllProfiles() {
        log.info("Called getAllProfiles");
        List<ProfileDTO> profiles = profileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ProfileDTO> updateProfileToClient(@PathVariable Long id,
                                            @Valid @RequestBody ProfileDTO profileDTO) {
        log.info("Called updateProfileToClient with id: {}", id);
        ProfileDTO profile = profileService.updateProfile(id, profileDTO);
        return ResponseEntity.ok(profile);
    }
}
