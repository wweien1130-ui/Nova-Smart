package com.liang.agent.controller;

import com.hmall.common.utils.UserContext;
import com.hmall.common.utils.JwtTool;
import com.liang.agent.domain.vo.ChatSessionVO;
import com.liang.agent.domain.vo.MessageVO;
import com.liang.agent.domain.vo.SessionVO;
import com.liang.agent.service.ChatSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private  final ChatSessionService chatSessionService;
    private final JwtTool jwtTool;

    /**
     * 新建会话
     * **/
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n",defaultValue = "3") Integer n,
                                   @RequestParam(value = "userId", required = false) Long paramUserId,
                                   HttpServletRequest request){
        // 1. 尝试从请求头 authorization 中解析 token 获取 userId
        Long userId = null;
        String token = request.getHeader("authorization");
        if (token != null && !token.isEmpty()) {
            try {
                userId = jwtTool.parseToken(token);
                log.debug("从 token 解析 userId: {}", userId);
            } catch (Exception e) {
                log.warn("token 解析失败: {}", e.getMessage());
            }
        }

        // 2. 如果 token 解析失败，尝试从请求参数获取 userId（前端降级方案）
        if (userId == null && paramUserId != null) {
            userId = paramUserId;
            log.debug("从请求参数获取 userId: {}", userId);
        }

        // 3. 如果还是没有 userId，尝试从 UserContext 获取（兼容已有登录流程）
        if (userId == null) {
            userId = UserContext.getUser();
        }

        return this.chatSessionService.createSession(n, userId);
    }


    /**
     * 获取热门会话
     * **/
    @GetMapping("/hot")
    public List<SessionVO.Example> hotExamples(@RequestParam(value = "n",defaultValue = "3") Integer n){
                return this.chatSessionService.hotExamples(n);
    }


    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return this.chatSessionService.queryBySessionId(sessionId);
    }


    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        return this.chatSessionService.queryHistorySession();
    }


    /**
     * 删除历史会话列表
     */
    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        this.chatSessionService.deleteHistorySession(sessionId);
    }



    /**
     * 更新历史会话标题
     */
    @PutMapping("/history")
    public void updateTitle(@RequestParam("sessionId") String sessionId,
                            @RequestParam("title") String title) {
        this.chatSessionService.updateTitle(sessionId, title);
    }

}
