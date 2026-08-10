package com.cakedelight.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Declares order.completed explicitly rather than leaving it to the broker's
// auto.create.topics.enable default (Spring Boot auto-configures a
// KafkaAdmin bean once spring.kafka.bootstrap-servers is set, which applies
// any NewTopic beans on startup — idempotent if the topic already exists).
// Single broker, single partition — matches this project's stated
// single-broker-locally assumption (CLAUDE.md §12); revisit both numbers if
// that ever changes.
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.order-completed}")
    private String topicName;

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
