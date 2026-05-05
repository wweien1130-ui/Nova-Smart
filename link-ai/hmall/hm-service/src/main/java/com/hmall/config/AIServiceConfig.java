package com.hmall.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class AIServiceConfig {

    @Value("${DASHSCOPE_API_KEY}")
    private String dashscopeApiKey;

    @Bean
    public ChatModel dashScopeChatModel() {
        return DashScopeChatModel.builder()
                .dashScopeApi(
                        DashScopeApi.builder()
                                .apiKey(dashscopeApiKey)
                                .baseUrl("https://dashscope.aliyuncs.com")
                                .build()
                )
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel("deepseek-v4-pro")
                                .build()
                )
                .build();
    }


    @Value("${DASHSCOPE_API_KEY}")
    private  String openapiKey;
    @Bean
    public ChatModel openapiChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .apiKey(openapiKey)
                                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                                .build()
                )
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("qwen3.6-flash-2026-04-16")
                                .build()
                )
                .build();
    }
}