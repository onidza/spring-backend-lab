package com.onidza.backend.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaTopicsProperties {

    private List<TopicConfig> topics = List.of();

    @Getter
    @Setter
    public static class TopicConfig {
        private String name;
        private int partitions = 3;
        private short replicationFactor = 1;
    }
}
