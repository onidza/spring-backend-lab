package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{id}/profile")
    public ProfileDTO getProfile(@PathVariable Long id) {
        return profileService.getProfileById(id);
    }

    @GetMapping("/profiles")
    public List<ProfileDTO> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @PutMapping("/{id}/profile")
    public ProfileDTO updateProfileToClient(@PathVariable Long id,
                                            @Valid @RequestBody ProfileDTO profileDTO) {
        return profileService.updateProfile(id, profileDTO);
    }
}
