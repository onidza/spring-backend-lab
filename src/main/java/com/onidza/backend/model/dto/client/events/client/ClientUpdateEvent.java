package com.onidza.backend.model.dto.client.events.client;

import com.onidza.backend.model.dto.client.events.ActionPart;

import java.util.EnumSet;
import java.util.Set;

public record ClientUpdateEvent(
        Long clientId,
        Long profileId
) {}
