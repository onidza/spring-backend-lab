package com.onidza.backend.model.events.client;

import java.util.EnumSet;

public record ClientAddEvent(
        EnumSet<ActionPart> parts
) {
}
