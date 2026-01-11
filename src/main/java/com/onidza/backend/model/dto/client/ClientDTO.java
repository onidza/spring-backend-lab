package com.onidza.backend.model.dto.client;

import com.onidza.backend.model.dto.CouponDTO;
import com.onidza.backend.model.dto.ProfileDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.validation.UniqueEmail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.onidza.backend.model.entity.Client;
import lombok.Generated;

import java.time.LocalDateTime;
import java.util.List;

/**
* DTO for {@link Client}
* */

@Generated
public record ClientDTO(
        Long id,

        @NotNull(message = "Name can't be empty")
        @Size(min = 2, max = 50, message = "Name must have length from 2 to 50 symbols")
        String name,

        @NotNull(message = "Email can't be empty")
        @Size(min = 2, max = 50, message = "Email must have length from 2 to 50 symbols")
        @UniqueEmail
        @Email(message = "Email must be valid")
        String email,

        LocalDateTime registrationDate,

        @Valid
        @NotNull(message = "Profile can't be empty")
        ProfileDTO profile,

        @Valid
        List<OrderDTO> orders,

        @Valid
        List<CouponDTO> coupons
) {}
