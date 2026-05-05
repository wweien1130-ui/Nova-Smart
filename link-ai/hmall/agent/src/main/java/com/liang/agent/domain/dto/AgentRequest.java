package com.liang.agent.domain.dto;

import lombok.Data;

@Data
public class AgentRequest {

    /**
     * 用户发送的消息
     * **/
    private String message;

    /***
     * 回话ID
     * */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;
}
