package com.liang.agent.controller;

import com.liang.agent.domain.dto.AgentRequest;
import com.liang.agent.domain.vo.ChatEventVo;
import com.liang.agent.service.Impl.RouterAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final RouterAgentService routerAgentService;

    @PostMapping("/chat")
    public Flux<ChatEventVo> chat(@RequestBody AgentRequest request) {
        String sessionId = request.getSessionId() != null ?
                          request.getSessionId() : String.valueOf(System.currentTimeMillis());

        return routerAgentService.chat(request.getMessage(), sessionId, request.getUserId());
    }

}
