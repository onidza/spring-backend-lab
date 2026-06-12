package com.onidza.backend.model.events.order;

public record OrderUpdateEvent(
    Long clientId,
    Long orderId
) {
}
