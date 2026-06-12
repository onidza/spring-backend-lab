package com.onidza.backend.model.events.order;

public record OrderDeleteEvent(
    Long clientId,
    Long orderId
) {
}
