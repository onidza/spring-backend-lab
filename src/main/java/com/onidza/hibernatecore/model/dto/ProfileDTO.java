package com.onidza.hibernatecore.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileDTO(
        Long id,

        @NotNull(message = "address must be not empty")
        @Size(min = 10, max = 100, message = "Address must have length from 10 to 100 symbols")
        String address,

        @NotNull(message = "phone must be not empty")
        @Pattern(
                regexp = "\\+?[0-9]{11}",
                message = "Phone number must have 11 digests")
        String phone,

        Long clientId
) {}
