package com.liang.agent.memory;

import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

@Data
public class MyMessage {

    private String messageType;          // 消息类型：SYSTEM / USER / ASSISTANT / TOOL
    private Map<String, Object> metadata = Map.of();      // 元数据
    private List<Media> media = List.of();          // 媒体内容
    private List<AssistantMessage.ToolCall> toolCalls = List.of();      // AI调用的工具列表
    private String textContent;      // 文本内容
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of();          // 工具返回结果
    private Map<String, Object> params = Map.of();        // 工具调用参数（扩展字段）

}
