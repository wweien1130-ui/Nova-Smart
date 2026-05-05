package com.liang.agent;


import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootTest(classes = AgentApplication.class)
public class AgentTest {


    @Autowired
    @Qualifier("dashScopeChatModel")
    private ChatModel dashScopeChatModel;

    @Autowired
    @Qualifier("openapiChatModel")
    private  ChatModel openapiChatModel;

    @Test
    public void testDashScopeChatModel() {
        // 测试DashScope聊天模型
        String question = "你是谁";
        String response = dashScopeChatModel.call(question);
        System.out.println("DashScope 响应: " + response);
    }

    @Test
    public void testOpenAIChatModel() {
        // 测试OpenAI聊天模型
        String question = "介绍一下你自己";
        String response = openapiChatModel.call(question);
        System.out.println("OpenAI 响应: " + response);
    }
}