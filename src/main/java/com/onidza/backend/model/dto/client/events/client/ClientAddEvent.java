package com.onidza.backend.model.dto.client.events.client;

import com.onidza.backend.model.dto.client.events.ActionPart;

import java.util.EnumSet;

public record ClientAddEvent(
        EnumSet<ActionPart> parts
) {}
