package com.onidza.backend.model.events.client;

import com.onidza.backend.model.events.ActionPart;

import java.util.EnumSet;

public record ClientAddEvent(
        EnumSet<ActionPart> parts
) {
}
