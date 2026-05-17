package com.hmall.service.mq;

import com.hmall.common.constants.KafkaConstants;
import com.hmall.common.domain.msg.UserActionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendUserAction(UserActionMessage message) {
        String json = String.format(
            "{\"userId\":%d,\"actionType\":\"%s\",\"targetType\":\"%s\",\"targetId\":%d,\"timestamp\":\"%s\"}",
            message.getUserId(),
            message.getActionType(),
            message.getTargetType(),
            message.getTargetId(),
            message.getTimestamp()
        );
        
        log.info("发送用户行为到 Kafka: userId={}, actionType={}", 
            message.getUserId(), message.getActionType());
        
        kafkaTemplate.send(KafkaConstants.TOPIC_USER_ACTION, 
            message.getUserId().toString(), json);
    }
}