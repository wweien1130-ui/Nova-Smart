package com.liang.agent.service.Impl;

import com.liang.agent.domain.vo.ChatEventVo;
import com.liang.agent.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RouterAgentService routerAgentService;

    @Override
    public Flux<ChatEventVo> chat(String question, String sessionId, Long userId) {
        // 直接交给 Router Agent 处理
        return routerAgentService.chat(question, sessionId, userId);
    }

    @Override
    public void stop(String sessionId) {
        // 交给 Router Agent 处理停止
        routerAgentService.stop(sessionId);
    }

}
