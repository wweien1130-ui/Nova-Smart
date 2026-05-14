package com.liang.agent.memory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class MyAssistantMessage extends AssistantMessage {

    /*
    * 在 AI Agent 场景中，AssistantMessage（AI 的回复）通常会包含 toolCalls（调用的工具），
    * 但工具调用时传入的原始参数会丢失。
    * 这个 params 就是用来保存 AI 调用工具时传入的原始参数，便于后续回溯或调试。
    * */

    private Map<String, Object> params;      // 工具调用参数（扩展字段）

    public MyAssistantMessage(String content, Map<String, Object> properties, List<ToolCall> toolCalls, List<Media> media, Map<String, Object> params) {
        super(content, properties, toolCalls, media);
        this.params = params;
    }

}
