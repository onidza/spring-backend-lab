package com.onidza.backend.model.events.client;

public record ClientUpdateEvent(
        Long clientId,
        Long profileId
) {
}
