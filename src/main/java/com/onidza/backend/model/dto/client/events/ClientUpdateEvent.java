package com.onidza.backend.model.dto.client.events;

import com.onidza.backend.model.dto.client.ClientActionPart;

import java.util.EnumSet;
import java.util.Set;

public record ClientUpdateEvent(
        Long profileId,
        EnumSet<ClientActionPart> parts,
        Set<Long> orderIdsToEvict,
        Set<Long> couponIdsToEvict
) {}
