package com.onidza.backend.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.dto.order.OrderCreateEvent;
import com.onidza.backend.model.entity.RetryableTask;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class, RetryableTaskStatus.class})
public interface RetryableTaskMapper {

    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(source = "event", target = "payload", qualifiedByName = "convertEventToJson")
    @Mapping(target = "status", expression = "java(RetryableTaskStatus.IN_PROGRESS)")
    @Mapping(target = "retryTime", expression = "java(Instant.now())")
    RetryableTask toRetryableTask(OrderCreateEvent event, RetryableTaskType type, @Context ObjectMapper objectMapper);

    @Named("convertEventToJson")
    default String convertEventToJson(OrderCreateEvent event, @Context ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting OrderCreateEvent to JSON", e);
        }
    }

    @Named("convertJsonToEvent")
    default OrderCreateEvent convertJsonToEvent(String json, @Context ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, OrderCreateEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to OrderCreateEvent", e);
        }
    }
}
