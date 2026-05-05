package com.liang.agent.domain.dto;

import lombok.Data;

@Data
public class AgentResponse {
    /***
     * AI的回复内容
     * */
    private String content;

    /**
     * 回话ID
     * **/
    private String sessionId;

    /**
     * 是否执行了工具调用
     * **/
    private boolean toolCalled;

    /**
     * 错误信息
     * **/
    private String error;

    public static AgentResponse success(String content){
        AgentResponse resp = new AgentResponse();
        resp.setContent(content);
        return resp;
    }

    public static AgentResponse error(String error){
        AgentResponse resp = new AgentResponse();
        resp.setError(error);
        return resp;
    }
}
