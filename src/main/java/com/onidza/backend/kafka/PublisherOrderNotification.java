package com.onidza.backend.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.kafka.AppKafkaTopicsProperties;
import com.onidza.backend.service.retryabletask.RetryableTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublisherOrderNotification {

    private final RetryableTaskService retryableTaskService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppKafkaTopicsProperties properties;

    public <T> void sendNotificationInTopic(UUID uuid, T payload) {
        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload of type " +
                    payload.getClass().getName(), e);
        }

        String topic = properties.getTopics().get(0).getName();

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, uuid.toString(), message);

        future.whenComplete((res, ex) -> {
            if (ex == null) {
                retryableTaskService.markSent(uuid);
                log.info("Task {} was send in topic {}", uuid, topic);
            }
        });
    }
}
