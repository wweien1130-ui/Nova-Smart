package com.liang.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.liang.agent.memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

@Configuration
public class AIServiceConfig {

    @Value("${DASHSCOPE_API_KEY}")
    private String dashscopeApiKey;

    @Value("${tj.ai.memory.max:100}")
    private Integer maxMessages;

    /** DashScope 模型名，从 Nacos 配置 tj.ai.models.dashscope 读取 */
    @Value("${tj.ai.models.dashscope}")
    private String dashscopeModel;

    /** OpenAI 模型名，从 Nacos 配置 tj.ai.models.openai 读取 */
    @Value("${tj.ai.models.openai}")
    private String openaiModel;

    // ===== ChatModel 配置 =====

    // DashScope聊天客户端
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
                                .withModel(dashscopeModel)
                                .build()
                )
                .build();
    }

    // OpenAI兼容模式聊天客户端
    @Bean
    public ChatModel openapiChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .apiKey(dashscopeApiKey)
                                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                                .build()
                )
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(openaiModel)
                                .build()
                )
                .build();
    }


    // ===== 聊天记忆（Redis）配置 =====

    /**
     * 基于 Redis 的 ChatMemoryRepository 实现
     * 用于将聊天记录持久化到 Redis 中
     */
    @Bean
    public ChatMemoryRepository redisChatMemoryRepository() {
        // 添加空值检查，避免因 Redis 配置问题导致应用启动失败
        try {
            return new RedisChatMemoryRepository();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create RedisChatMemoryRepository. " +
                    "Please check your Redis configuration in Nacos common.yaml", e);
        }
    }

    /**
     * 基于 MessageWindowChatMemory 的聊天记忆
     * 使用 RedisChatMemoryRepository 作为底层存储，
     * 并限制最大消息数量，超出时自动删除最旧的对话
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(this.maxMessages)
                .build(); // 滑动窗口自动管理消息数量
    }

}
