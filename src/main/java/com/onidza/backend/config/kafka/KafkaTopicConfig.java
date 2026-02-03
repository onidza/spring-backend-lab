package com.onidza.backend.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildAdminProperties(sslBundles));
        return new KafkaAdmin(config);
    }

    @Bean
    public List<NewTopic> newTopics(AppKafkaTopicsProperties props) {
        return props.getTopics().stream()
                .map(t -> new NewTopic(t.getName(), t.getPartitions(), t.getReplicationFactor()))
                .toList();
    }
}
