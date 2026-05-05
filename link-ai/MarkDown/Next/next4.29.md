# 后续开发三阶段规划（2026-04-29）

> **背景**：参考天机学堂项目的 AI 助手实现方案，结合当前 LinkAI 项目 agent 模块的现状，制定分阶段实施计划。

---

## 目录

1. [当前项目现状](#1-当前项目现状)
2. [阶段一：核心存储与序列化（P0）](#2-阶段一核心存储与序列化p0)
3. [阶段二：会话管理与接口完善（P1）](#3-阶段二会话管理与接口完善p1)
4. [阶段三：配置外置与体验优化（P2）](#4-阶段三配置外置与体验优化p2)
5. [完整功能对照表](#5-完整功能对照表)

---

## 1. 当前项目现状

### ✅ 已实现功能

| 功能 | 文件 | 说明 |
|------|------|------|
| agent 独立模块 | `hmall/agent/` | 独立微服务，可单独部署 |
| 双模型支持 | `AIServiceConfig.java` | DashScope + OpenAI 兼容模式 |
| SSE 流式输出 | `AgentController.java` | 返回 `Flux<String>` 原始流 |
| 请求 DTO | `AgentRequest.java` | 含 `message` + `sessionId` |
| 响应 DTO | `AgentResponse.java` | 含 `content`、`sessionId`、`toolCalled`、`error` |
| ChatHistoryRepository | `ChatHistoryRepository.java` | 接口 + InMemory 实现 |
| 商品搜索 Tool | `ItemSearchTool.java` | searchItems + getItemDetail |
| System Prompt | `SystemConstants.java` | 完整的购物助手提示词 |
| 内存级 ChatMemory | `AIServiceConfig.java` | `MessageWindowChatMemory` |
| MessageChatMemoryAdvisor | `AIServiceConfig.java` | 已集成到 ChatClient |

### ❌ 缺失功能（需要补充）

| 功能 | 优先级 | 说明 |
|------|--------|------|
| Redis 会话记忆 | 🔴 P0 | 替换内存级 ChatMemory |
| 消息序列化/反序列化 | 🔴 P0 | 解决 Redis 存储 Message 问题 |
| 流式中断时保存 | 🔴 P0 | doOnCancel 手动保存 |
| chat_session 表 | 🟡 P1 | 会话持久化 |
| SessionController | 🟡 P1 | 创建会话、查询会话列表 |
| 查询会话历史消息 | 🟡 P1 | 按 sessionId 查询对话记录 |
| Nacos 配置中心 | 🟢 P2 | 提示词模板外置 |
| 预热功能（热门问题） | 🟢 P2 | 3条模板问题显示 |
| 停止生成功能 | 🟢 P2 | 中断 Flux 流输出 |
| 枚举类（DATA/STOP/PARAM） | 🟢 P2 | 事件类型标识 |

---

## 2. 阶段一：核心存储与序列化（P0）

> **目标**：解决最核心的 Redis 会话记忆存储问题，替换当前的内存级实现

### 2.1 创建 MyMessage 自定义消息类

**问题**：Spring AI 官方的 `Message` 接口实现类（如 `AssistantMessage`、`UserMessage`）中，`textContent` 属性没有提供 get 方法，导致 Redis 序列化时无法获取文本内容。

**实现**：
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyMessage {
    private String messageType;    // 消息类型：user/assistant/system/tool
    private String textContent;    // 文本内容
    private Map<String, Object> metadata;  // 元数据
}
```

### 2.2 创建 MessageUtil 工具类

**功能**：实现 `Message` ↔ `MyMessage` 的互相转换，以及序列化/反序列化

```java
public class MessageUtil {
    // Message → MyMessage（序列化）
    public static MyMessage fromMessage(Message message) { ... }
    
    // MyMessage → Message（反序列化）
    public static Message toMessage(MyMessage myMessage) { ... }
    
    // List<Message> → List<MyMessage>
    public static List<MyMessage> fromMessages(List<Message> messages) { ... }
    
    // List<MyMessage> → List<Message>
    public static List<Message> toMessages(List<MyMessage> myMessages) { ... }
}
```

### 2.3 创建 RedisChatMemoryRepository

**功能**：实现 Spring AI 的 `ChatMemoryRepository` 接口，使用 Redis List 结构存储聊天记录

```java
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    // 使用 Redis List 存储，key = "chat:memory:{conversationId}"
    // 每个元素是序列化后的 MyMessage JSON 字符串
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) { ... }
    
    @Override
    public List<Message> get(String conversationId) { ... }
    
    @Override
    public void clear(String conversationId) { ... }
    
    // 扩展：查询所有会话ID
    public List<String> getConversationIds(String type) { ... }
}
```

### 2.4 替换 AIServiceConfig 中的 ChatMemory

```java
// 旧：内存级
@Bean
public ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder().build();
}

// 新：Redis 级
@Bean
public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
    return new ChatMemory(repository);
}
```

### 2.5 解决流式中断时保存问题

**问题**：调用 stop 接口中断 Flux 流后，Spring AI 不会触发 `ChatMemoryRepository.saveAll`，导致数据丢失。

**解决方案**：在 Flux 的 `doOnCancel` 回调中手动保存

```java
return chatClient.prompt()
    .system(systemPrompt)
    .user(request.getMessage())
    .advisors(a -> a.param("chatId", sessionId))
    .stream()
    .content()
    .doOnCancel(() -> {
        // 手动保存助手回复到 Redis
        chatMemoryRepository.saveAll(conversationId, messages);
    });
```

### 2.6 涉及文件清单

| 文件 | 操作 |
|------|------|
| `agent/src/main/java/com/liang/agent/dto/MyMessage.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/utils/MessageUtil.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/repository/RedisChatMemoryRepository.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/config/AIServiceConfig.java` | ✏️ 修改 |
| `agent/src/main/java/com/liang/agent/controller/AgentController.java` | ✏️ 修改 |
| `agent/pom.xml` | ✏️ 添加 Redis 依赖 |

---

## 3. 阶段二：会话管理与接口完善（P1）

> **目标**：将会话持久化到数据库，完善会话管理接口

### 3.1 创建 chat_session 表

```sql
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID(UUID)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) DEFAULT NULL COMMENT '会话标题',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id)
) COMMENT '聊天会话表';
```

### 3.2 创建实体类与 Mapper

```java
@Data
@TableName("chat_session")
public class ChatSession {
    private Long id;
    private String sessionId;
    private Long userId;
    private String title;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long createdBy;
    private Long updatedBy;
}
```

### 3.3 创建 SessionController

| 接口 | 方法 | 说明 |
|------|------|------|
| `POST /agent/session/create` | createSession() | 创建新会话，返回 sessionId |
| `GET /agent/session/list` | listSessions() | 查询用户的会话列表 |
| `GET /agent/session/hot` | hotQuestions() | 获取热门问题（预热） |
| `GET /agent/session/{sessionId}/messages` | getMessages() | 查询会话的历史消息 |

### 3.4 创建枚举类

```java
public enum EventType {
    DATA(1001, "数据事件"),
    STOP(1002, "停止事件"),
    PARAM(1003, "参数事件");
    
    private final int value;
    private final String desc;
}
```

### 3.5 改造 SSE 响应格式

当前返回的是 `Flux<String>` 纯文本流，需要改为结构化 SSE 格式：

```json
// 数据事件
{"eventType": 1001, "eventData": "你好，我是小惠..."}

// 停止事件
{"eventType": 1002, "eventData": ""}

// 参数事件
{"eventType": 1003, "eventData": "{\"sessionId\": \"xxx\"}"}
```

### 3.6 涉及文件清单

| 文件 | 操作 |
|------|------|
| `agent/src/main/java/com/liang/agent/entity/ChatSession.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/mapper/ChatSessionMapper.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/service/ChatSessionService.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/service/impl/ChatSessionServiceImpl.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/controller/SessionController.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/enums/EventType.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/dto/ChatDTO.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/vo/SessionVO.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/vo/MessageVO.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/controller/AgentController.java` | ✏️ 修改（SSE 格式改造） |

---

## 4. 阶段三：配置外置与体验优化（P2）

> **目标**：将配置外置到 Nacos，提升用户体验

### 4.1 Nacos 配置中心集成

**现状**：System Prompt 硬编码在 `SystemConstants.java` 中

**改造**：
1. 在 Nacos 中创建配置文件 `agent-system-prompt.txt`
2. 使用 `@ConfigurationProperties(prefix = "tj.ai.session")` 读取配置
3. 支持热更新（`@RefreshScope`）

```yaml
# Nacos 配置内容
tj:
  ai:
    session:
      system-prompt: |
        你是一个智能购物助手，名字叫"小惠"...
      hot-questions:
        - "帮我推荐几款热销商品"
        - "如何查询我的订单？"
        - "有什么优惠活动吗？"
```

### 4.2 预热功能（热门问题）

**功能**：在聊天框上方显示 3 条模板问题，点击可直接发送

**实现**：
- 从 Nacos 读取配置的热门问题列表
- 随机选取 3 条返回给前端
- 支持"换一换"功能

### 4.3 停止生成功能

**功能**：用户点击停止按钮，中断 AI 输出

**实现**：
- 使用 `Map<String, Disposable>` 存储每个会话的 Flux 订阅
- 调用 `disposable.dispose()` 中断流
- 在 `doOnCancel` 中保存已生成的内容到 Redis

### 4.4 涉及文件清单

| 文件 | 操作 |
|------|------|
| `agent/src/main/java/com/liang/agent/config/AISessionProperties.java` | 🆕 新建 |
| `agent/src/main/java/com/liang/agent/controller/SessionController.java` | ✏️ 添加热门问题接口 |
| `agent/src/main/java/com/liang/agent/controller/AgentController.java` | ✏️ 添加停止接口 |
| `agent/src/main/resources/application.yaml` | ✏️ 添加 Nacos 配置 |
| `agent/pom.xml` | ✏️ 添加 Nacos 配置中心依赖 |

---

## 5. 完整功能对照表

| # | 功能 | 天机学堂方案 | 当前项目状态 | 阶段 | 预计工作量 |
|---|------|------------|------------|------|-----------|
| 1 | Redis 会话记忆 | `RedisChatMemoryRepository` | ❌ 内存级 | 阶段一 | ⭐⭐⭐ |
| 2 | 消息序列化 | `MyMessage` + `MessageUtil` | ❌ 未实现 | 阶段一 | ⭐⭐ |
| 3 | 流式中断保存 | `doOnCancel` 手动保存 | ❌ 未实现 | 阶段一 | ⭐ |
| 4 | chat_session 表 | 数据库表 + 实体类 | ❌ 未创建 | 阶段二 | ⭐ |
| 5 | SessionController | 创建/查询会话 | ❌ 未实现 | 阶段二 | ⭐⭐ |
| 6 | 查询历史消息 | 按 sessionId 查询 | ❌ 未实现 | 阶段二 | ⭐⭐ |
| 7 | SSE 结构化响应 | `eventType` + `eventData` | ⚠️ 纯文本流 | 阶段二 | ⭐ |
| 8 | 枚举类 | DATA/STOP/PARAM | ❌ 未创建 | 阶段二 | ⭐ |
| 9 | Nacos 配置中心 | 提示词 + 热门问题外置 | ❌ 硬编码 | 阶段三 | ⭐⭐ |
| 10 | 预热功能 | 3条模板问题 | ❌ 未实现 | 阶段三 | ⭐ |
| 11 | 停止生成 | Flux 中断 | ❌ 未实现 | 阶段三 | ⭐⭐ |

---

## 总结

```
阶段一（P0）：解决"存不住"的问题
  └── Redis 会话记忆 + 序列化 + 流式中断保存
      └── 这是最核心的基础设施

阶段二（P1）：解决"管不了"的问题
  └── 会话管理 + 接口完善 + SSE 格式化
      └── 让前端能正常交互

阶段三（P2）：解决"不好用"的问题
  └── Nacos 配置 + 预热 + 停止生成
      └── 提升运维和用户体验
```

> **建议**：当前先专注阶段一，把 Redis 会话记忆和序列化问题彻底解决，这是后续所有功能的基础。阶段一完成后，项目就具备了生产可用的会话记忆能力。
