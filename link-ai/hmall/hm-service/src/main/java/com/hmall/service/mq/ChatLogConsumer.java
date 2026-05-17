package com.hmall.service.mq;

import com.hmall.common.constants.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatLogConsumer {

    @KafkaListener(topics = KafkaConstants.TOPIC_CHAT_LOG, groupId = "chat-log-group")
    public void handleChatLog(String message) {
        log.info("收到聊天日志: {}", message);
    }
}