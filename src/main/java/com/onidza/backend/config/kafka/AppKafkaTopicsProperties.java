package com.onidza.backend.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaTopicsProperties {

    private Map<String, Topic> topics = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Topic {
        private String name;
        private int partitions;
        private short replicationFactor;
    }

    public Topic getTopic(String key) {
        return topics.get(key);
    }

    public Collection<Topic> getAllTopics() {
        return topics.values();
    }
}
