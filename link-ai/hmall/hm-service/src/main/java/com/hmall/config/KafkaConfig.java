package com.hmall.config;

import com.hmall.common.constants.KafkaConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic chatLogTopic() {
        return TopicBuilder.name(KafkaConstants.TOPIC_CHAT_LOG)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userActionTopic() {
        return TopicBuilder.name(KafkaConstants.TOPIC_USER_ACTION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditLogTopic() {
        return TopicBuilder.name(KafkaConstants.TOPIC_AUDIT_LOG)
                .partitions(3)
                .replicas(1)
                .build();
    }
}