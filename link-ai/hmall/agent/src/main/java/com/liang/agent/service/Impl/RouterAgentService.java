package com.liang.agent.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liang.agent.config.SystemPromptConfig;
import com.liang.agent.config.ToolResultHolder;
import com.liang.agent.constants.Constant;
import com.liang.agent.domain.vo.ChatEventVo;
import com.liang.agent.enums.AgentTypeEnum;
import com.liang.agent.enums.ChatEventTypeEnum;
import com.liang.agent.result.CartInfo;
import com.liang.agent.result.ItemInfo;
import com.liang.agent.service.ChatService;
import com.liang.agent.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {


//    @Qualifier("routerChatClient")
    private final ChatClient routerChatClient;

//    @Qualifier("itemChatClient")
    private final ChatClient itemChatClient;

//    @Qualifier("cartChatClient")
    private final ChatClient cartChatClient;

//    @Qualifier("orderChatClient")
    private final ChatClient orderChatClient;

//    @Qualifier("addressChatClient")
    private final ChatClient addressChatClient;

    private final ChatMemory chatMemory;

    private final ChatSessionService chatSessionService;

    private final SystemPromptConfig systemPromptConfig;

    // 存储大模型的生成状态
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> STOP_FLAG = new ConcurrentHashMap<>();

    // ===== 本地规则匹配（关键词子串匹配，更灵活） =====
    // 购物车相关：包含"购物车"、"加购"、"移除"等
    private static final List<String> KEYWORDS_CART = List.of(
            "购物车", "加购", "移除", "删除商品"
    );
    // 订单相关：包含"订单"、"下单"、"收货"等
    private static final List<String> KEYWORDS_ORDER = List.of(
            "订单", "下单", "收货"
    );
    // 地址相关：包含"地址"
    private static final List<String> KEYWORDS_ADDRESS = List.of(
            "地址"
    );
    // 确认语：简短确认
    private static final List<String> KEYWORDS_CONFIRM = List.of(
            "对", "好的", "可以", "嗯", "是的", "确定", "确认"
    );

    /**
     * 本地规则匹配：用关键词子串匹配判断意图，0 毫秒，不调 LLM
     * 使用 contains 子串匹配，比精确关键词更灵活
     * 例如："把这个商品加入到购物车中" 包含 "购物车" → CART
     */
    private AgentTypeEnum routeByKeywords(String question) {
        if (question == null || question.isBlank()) {
            return AgentTypeEnum.ITEM;
        }
        String q = question.trim();

        // 优先级：CART > ORDER > ADDRESS > ITEM
        if (containsAny(q, KEYWORDS_CART)) return AgentTypeEnum.CART;
        if (containsAny(q, KEYWORDS_ORDER)) return AgentTypeEnum.ORDER;
        if (containsAny(q, KEYWORDS_ADDRESS)) return AgentTypeEnum.ADDRESS;
        if (containsAny(q, KEYWORDS_CONFIRM)) return AgentTypeEnum.CART; // 确认语默认路由到 CART（加购）
        // 默认走 ITEM（商品搜索）
        return AgentTypeEnum.ITEM;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    /**
     * 路由处理入口
     *
     * @param question  用户问题
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 流式响应
     */
    public Flux<ChatEventVo> chat(String question, String sessionId, Long userId) {
        // 获取对话id
        String conversationId = ChatService.getConversationId(sessionId, userId);

        // 异步更新会话信息
        this.chatSessionService.update(sessionId, question, userId);

        // 1. 先用本地规则匹配意图（0 毫秒，不调 LLM）
        AgentTypeEnum agentType = routeByKeywords(question);
        log.info("本地规则路由: {} -> {}, 问题: {}", agentType.getAgentName(), agentType.getDesc(), question);

        // 2. 直接处理，跳过 LLM Router 调用
        return routeAndProcess(agentType, question, sessionId, userId, conversationId);
    }

    /**
     * 路由并处理（直接指定 agentType，跳过 LLM Router 调用）
     */
    private Flux<ChatEventVo> routeAndProcess(AgentTypeEnum agentType, String question, String sessionId, Long userId, String conversationId) {
        return Flux.create(sink -> {
            try {
                // 1. 获取对应的子 Agent ChatClient
                ChatClient targetClient = getTargetClient(agentType);

                // 2. 生成 requestId
                String requestId = IdUtil.fastSimpleUUID();

                // 3. 获取系统提示词（可能为 null，null 表示使用 ChatClient 的 defaultSystem）
                String systemText = getSystemPromptForAgent(agentType);

                // 大模型输出内容的缓冲器
                var outputBuilder = new StringBuilder();

                // 4. 调用子 Agent 处理（流式输出）
                var promptBuilder = targetClient.prompt();
                // 如果有自定义系统提示词则设置，否则使用 ChatClient 的 defaultSystem
                if (systemText != null && !systemText.isBlank()) {
                    promptBuilder.system(promptSpec -> promptSpec
                            .text(systemText)
                            .param("now", DateUtil.now()));
                }
                promptBuilder
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .toolContext(userId != null ?
                                Map.of(
                                        Constant.REQUEST_ID, requestId,
                                        Constant.USER_ID, userId
                                ) :
                                Map.of(
                                        Constant.REQUEST_ID, requestId
                                )
                        )
                        .user(question)
                        .stream()
                        .chatResponse()

                        // 生命周期管理
                        .doFirst(() -> {
                            GENERATE_STATUS.put(sessionId, true);
                            STOP_FLAG.put(sessionId, true);
                        })

                        .doOnNext(response -> {
                            outputBuilder.append(response.getResult().getOutput().getText());
                        })

                        .takeWhile(response -> STOP_FLAG.getOrDefault(sessionId, false))

                        .map(chatResponse -> {
                            // 处理 finishReason，保存消息ID到 ToolResultHolder
                            var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                            if (StrUtil.equals(Constant.STOP, finishReason)) {
                                var assistantMessage = chatResponse.getResult().getOutput();
                                var messageId = String.valueOf(assistantMessage.getMetadata().get(Constant.ID));
                                ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                            }

                            String text = chatResponse.getResult().getOutput().getText();
                            return ChatEventVo.builder()
                                    .eventData(text)
                                    .eventType(ChatEventTypeEnum.DATA.getValue())
                                    .build();
                        })

                        .doOnNext(event -> {
                            // 将每个事件发送到 sink
                            sink.next(event);
                        })

                        .doOnComplete(() -> {
                            // 发送 Tool 结果事件和结束标记
                            sendToolResultAndStop(sink, requestId);
                            GENERATE_STATUS.remove(sessionId);
                            STOP_FLAG.remove(sessionId);
                        })

                        .doOnError(throwable -> {
                            String content = outputBuilder.toString();
                            if (!content.isEmpty()) {
                                saveStopHistoryRecord(conversationId, content);
                            }
                            GENERATE_STATUS.remove(sessionId);
                            STOP_FLAG.remove(sessionId);
                            Throwable cause = throwable.getCause();
                            if (cause instanceof IOException || throwable instanceof ClientAbortException) {
                                log.debug("Client disconnected (normal): {}", cause != null ? cause.getMessage() : throwable.getMessage());
                            } else {
                                log.warn("Stream error: {}", throwable.getMessage());
                            }
                            sink.error(throwable);
                        })

                        .doOnCancel(() -> {
                            String content = outputBuilder.toString();
                            if (!content.isEmpty()) {
                                saveStopHistoryRecord(conversationId, content);
                            }
                            STOP_FLAG.remove(sessionId);
                            GENERATE_STATUS.remove(sessionId);
                            log.debug("Stream cancelled for session: {}", sessionId);
                        })

                        .subscribe();

            } catch (Exception e) {
                log.error("Router 处理失败", e);
                sink.error(e);
            }
        });
    }


    /**
     * 发送 Tool 结果事件和停止事件
     */
    private void sendToolResultAndStop(FluxSink<ChatEventVo> sink, String requestId) {
        var map = ToolResultHolder.get(requestId);
        if (CollUtil.isNotEmpty(map)) {
            boolean hasCartInfo = map.values().stream()
                    .anyMatch(v -> v instanceof CartInfo);
            boolean hasItemInfo = map.values().stream()
                    .anyMatch(v -> v instanceof ItemInfo);

            ChatEventTypeEnum eventType = hasCartInfo ? ChatEventTypeEnum.CART : ChatEventTypeEnum.PARAM;

            var eventVO = ChatEventVo.builder()
                    .eventData(map)
                    .eventType(eventType.getValue())
                    .build();

            sink.next(eventVO);
        }

        // 发送停止事件
        sink.next(ChatEventVo.builder()
                .eventType(ChatEventTypeEnum.STOP.getValue())
                .build());

        sink.complete();
    }

    /**
     * 从 Router 返回的文本中提取 JSON 字符串
     * 大模型可能会返回 markdown 代码块包裹的 JSON，如 ```json\n{...}\n```
     */
    private String extractJsonFromResponse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();

        // 尝试提取 ```json ... ``` 包裹的内容
        int jsonStart = trimmed.indexOf("```json");
        if (jsonStart != -1) {
            int contentStart = jsonStart + 7; // 跳过 ```json
            int jsonEnd = trimmed.indexOf("```", contentStart);
            if (jsonEnd != -1) {
                String extracted = trimmed.substring(contentStart, jsonEnd).trim();
                if (extracted.startsWith("{")) {
                    return extracted;
                }
            }
        }
        // 尝试提取 ``` ... ``` 包裹的内容
        jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int contentStart = jsonStart + 3;
            int jsonEnd = trimmed.indexOf("```", contentStart);
            if (jsonEnd != -1) {
                String extracted = trimmed.substring(contentStart, jsonEnd).trim();
                if (extracted.startsWith("{")) {
                    return extracted;
                }
            }
        }
        // 没有代码块包裹，直接检查是否以 { 开头
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        // 尝试查找第一个 { 和最后一个 } 来提取 JSON
        int firstBrace = trimmed.indexOf("{");
        int lastBrace = trimmed.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        }
        // 实在找不到 JSON，返回 null 让调用方处理
        log.warn("无法从 LLM 响应中提取 JSON: {}", text.substring(0, Math.min(100, text.length())));
        return null;
    }


    /**
     * 解析 Router 返回的 JSON，提取 agentType
     */
    private AgentTypeEnum parseRouterResult(String json) {
        try {
            String cleanJson = extractJsonFromResponse(json);
            if (cleanJson == null) {
                log.warn("Router 返回结果为空: {}", json);
                return AgentTypeEnum.ITEM;
            }
            JSONObject jsonObj = JSONUtil.parseObj(cleanJson);
            String agentTypeStr = jsonObj.getStr("agentType");
            if (agentTypeStr == null) {
                log.warn("Router 返回结果中缺少 agentType 字段: {}", cleanJson);
                return AgentTypeEnum.ITEM; // 默认路由到商品搜索
            }
            AgentTypeEnum type = AgentTypeEnum.agentNameOf(agentTypeStr);
            if (type == null) {
                log.warn("未知的智能体类型: {}, 使用默认 ITEM", agentTypeStr);
                return AgentTypeEnum.ITEM;
            }
            return type;
        } catch (Exception e) {
            log.error("解析 Router 返回结果失败: {}, 使用默认 ITEM", json, e);
            return AgentTypeEnum.ITEM;
        }
    }

    /**
     * 解析 Router 返回的 JSON，提取 question
     */
    private String parseRouterQuestion(String json) {
        try {
            String cleanJson = extractJsonFromResponse(json);
            if (cleanJson == null) {
                return null;
            }
            JSONObject jsonObj = JSONUtil.parseObj(cleanJson);
            return jsonObj.getStr("question");
        } catch (Exception e) {
            log.error("解析 Router 返回的 question 失败", e);
            return null;
        }
    }

    /**
     * 根据智能体类型获取对应的 ChatClient
     */
    private ChatClient getTargetClient(AgentTypeEnum agentType) {
        return switch (agentType) {
            case ITEM -> itemChatClient;
            case CART -> cartChatClient;
            case ORDER -> orderChatClient;
            case ADDRESS -> addressChatClient;
            default -> {
                log.warn("未知的智能体类型: {}, 使用 ITEM", agentType);
                yield itemChatClient;
            }
        };
    }

    /**
     * 获取智能体的系统提示词
     * 返回 null 表示使用 ChatClient 的 defaultSystem
     */
    private String getSystemPromptForAgent(AgentTypeEnum agentType) {
        // 对于非 ITEM 类型的智能体，使用各自的默认提示词（已在 RouterAgentConfig 中配置）
        // 对于 ITEM 类型，尝试从 Nacos 获取动态提示词
        if (agentType == AgentTypeEnum.ITEM) {
            String systemText = systemPromptConfig.getChatSystemMessage().get();
            if (systemText != null && !systemText.isBlank()) {
                return systemText;
            }
        }
        // 返回 null，使用 ChatClient 的 defaultSystem
        return null;
    }

    /**
     * 停止生成
     */
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
        STOP_FLAG.remove(sessionId);
    }

    /**
     * 保存停止输出的记录
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

}
