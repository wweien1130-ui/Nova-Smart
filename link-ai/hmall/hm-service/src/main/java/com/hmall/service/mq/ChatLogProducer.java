package com.hmall.service.mq;

import com.hmall.common.constants.KafkaConstants;
import com.hmall.common.domain.msg.ChatLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendChatLog(ChatLogMessage message) {
        String json = String.format(
            "{\"userId\":%d,\"sessionId\":\"%s\",\"question\":\"%s\",\"answer\":\"%s\",\"timestamp\":\"%s\"}",
            message.getUserId(),
            message.getSessionId(),
            message.getQuestion(),
            message.getAnswer(),
            message.getTimestamp()
        );
        
        log.info("发送聊天日志到 Kafka: userId={}, sessionId={}", 
            message.getUserId(), message.getSessionId());
        
        kafkaTemplate.send(KafkaConstants.TOPIC_CHAT_LOG, message.getSessionId(), json);
    }
}