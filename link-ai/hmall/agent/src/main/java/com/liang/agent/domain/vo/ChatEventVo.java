package com.liang.agent.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatEventVo {

    /**
     * 文本内容
     * **/
    private Object eventData;

    /**
     * 事件类型，1001-数据事件 ， 1002-停止事件 ， 1003-参数事件
     * **/
    private int eventType;


}
