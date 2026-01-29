package com.onidza.backend.model.dto.client.events;

import com.onidza.backend.model.dto.client.ClientActionPart;

import java.util.EnumSet;

public record ClientAddEvent(
        EnumSet<ClientActionPart> parts
) {}
