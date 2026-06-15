package com.onidza.backend.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin.NewTopics newTopics(AppKafkaTopicsProperties props) {
        return new KafkaAdmin.NewTopics(
                props.getAllTopics()
                        .stream()
                        .map(topic -> TopicBuilder
                                .name(topic.getName())
                                .partitions(topic.getPartitions())
                                .replicas(topic.getReplicationFactor())
                                .build()
                        ).toArray(NewTopic[]::new)
        );
    }
}
