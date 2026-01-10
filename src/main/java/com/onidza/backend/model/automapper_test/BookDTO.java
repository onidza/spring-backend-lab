package com.onidza.backend.model.automapper_test;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Generated;

/**
 * DTO for {@link Book}
 * */

@Generated
public record BookDTO (
        Long id,

        @NotNull(message = "Name can't be null")
        @Size(min = 3, max = 50, message = "Name must have length from 3 to 50 symbols")
        String name,

        @NotNull(message = "Author name can't be null")
        @Size(min = 3, max = 25, message = "Author name must have length from 3 to 25 symbols")
        String author
) {}
