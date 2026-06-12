package com.onidza.backend.model.events.client;

import com.onidza.backend.model.events.ActionPart;

import java.util.EnumSet;
import java.util.Set;

public record ClientDeletedEvent(
        Long clientId,
        Long profileId,
        Set<Long> orderIdsToEvict,
        Set<Long> couponIdsToEvict,
        EnumSet<ActionPart> parts
) {
}
