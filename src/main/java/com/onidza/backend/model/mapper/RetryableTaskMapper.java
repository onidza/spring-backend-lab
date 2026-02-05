package com.onidza.backend.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.entity.Order;
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
    @Mapping(source = "order", target = "payload", qualifiedByName = "convertObjectToJson")
    @Mapping(target = "status", expression = "java(RetryableTaskStatus.IN_PROGRESS)")
    RetryableTask toRetryableTask(Order order, RetryableTaskType type, @Context ObjectMapper objectMapper);

    @Named("convertObjectToJson")
    default String convertObjectToJson(Order order, @Context ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting Order to JSON", e);
        }
    }

    @Named("convertJsonToOrder")
    default Order convertJsonToOrder(String json, @Context ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, Order.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to Order", e);
        }
    }
}
