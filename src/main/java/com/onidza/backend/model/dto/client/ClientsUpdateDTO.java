package com.onidza.backend.model.dto.client;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.util.validation.UniqueEmail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientsUpdateDTO(
        @NotNull(message = "Name can't be empty")
        @Size(min = 2, max = 50, message = "Name must have length from 2 to 50 symbols")
        String name,

        @NotNull(message = "Email can't be empty")
        @Size(min = 2, max = 50, message = "Email must have length from 2 to 50 symbols")
        @UniqueEmail
        @Email(message = "Email must be valid")
        String email,

        @Valid
        @NotNull(message = "Profile can't be empty")
        ProfileDTO profile
) {
}
