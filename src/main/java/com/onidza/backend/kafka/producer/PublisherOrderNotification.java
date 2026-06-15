package com.onidza.backend.kafka.producer;

import com.onidza.backend.config.kafka.AppKafkaTopicsProperties;
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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppKafkaTopicsProperties properties;

    public CompletableFuture<SendResult<String, String>> sendNotificationInTopic(
            UUID uuid,
            String payload
    ) {
        String topic = properties.getTopic("order-notification").getName();

        return kafkaTemplate.send(topic, uuid.toString(), payload);
    }
}
