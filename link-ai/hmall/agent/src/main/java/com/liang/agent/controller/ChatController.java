package com.liang.agent.controller;


import com.hmall.common.annotation.NoWrapper;
import com.hmall.common.utils.JwtTool;
import com.hmall.common.utils.UserContext;
import com.liang.agent.domain.dto.ChatDTO;
import com.liang.agent.domain.vo.ChatEventVo;
import com.liang.agent.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final JwtTool jwtTool;

    @NoWrapper //标记结果不进行包装
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVo> chat(@RequestBody ChatDTO chatDTO, HttpServletRequest request) {
        // 1. 优先使用前端传入的 userId
        Long userId = chatDTO.getUserId();

        // 2. 如果前端没有传入 userId，尝试从请求头 authorization 中解析 token 获取 userId
        if (userId == null) {
            String token = request.getHeader("authorization");
            if (token != null && !token.isEmpty()) {
                try {
                    userId = jwtTool.parseToken(token);
                    log.debug("从 token 解析 userId: {}", userId);
                } catch (Exception e) {
                    log.warn("token 解析失败: {}", e.getMessage());
                }
            }
        }

        // 3. 如果还是没有 userId，尝试从 UserContext 获取（兼容已有登录流程）
        if (userId == null) {
            userId = UserContext.getUser();
        }

        return this.chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId(), userId);
    }

    @PostMapping("/stop")
    public void stop(@RequestParam("sessionId")  String sessionId) {
        this.chatService.stop(sessionId);
    }
}
