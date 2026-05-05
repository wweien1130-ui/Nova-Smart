package com.liang.agent.service;


import com.hmall.common.utils.UserContext;
import com.liang.agent.domain.vo.ChatEventVo;
import reactor.core.publisher.Flux;

public interface ChatService {
    /**
     * 聊天
     *
     * @param question  问题
     * @param sessionId 会话id
     * @param userId    用户id
     * @return 回答内容
     */
    Flux<ChatEventVo> chat(String question, String sessionId, Long userId);

    /**
     * 聊天（兼容无 userId 的调用）
     */
    default Flux<ChatEventVo> chat(String question, String sessionId) {
        return chat(question, sessionId, UserContext.getUser());
    }

    /**
     * 停止聊天
     * @param sessionId 会话id
     * **/
    void stop(String sessionId);



    /**
     * 获取对话id，规则：用户id_会话id
     *
     * @param sessionId 会话id
     * @param userId    用户id
     * @return 对话id
     */
    static String getConversationId(String sessionId, Long userId) {
        return (userId != null ? userId : 0) + "_" + sessionId;
    }
}
