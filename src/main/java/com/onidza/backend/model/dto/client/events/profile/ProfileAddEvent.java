package com.onidza.backend.model.dto.client.events.profile;

import com.onidza.backend.model.dto.client.events.ActionPart;

import java.util.EnumSet;

public record ProfileAddEvent(
        Long clientId,
        EnumSet<ActionPart> parts
) {}
