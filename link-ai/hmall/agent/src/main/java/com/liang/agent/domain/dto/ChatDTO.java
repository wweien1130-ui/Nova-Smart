package com.liang.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {

    /**
     * 用户的问题
     * **/
    private String question;

    /**
     * 会话id
     * **/
    private String sessionId;

    /**
     * 用户ID（前端登录后传入）
     * **/
    private Long userId;
}
