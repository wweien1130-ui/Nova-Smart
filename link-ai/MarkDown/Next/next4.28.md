# 电商项目 AI Agent 智能化改造大纲

## 📌 项目背景

当前项目是一个基于 Spring Boot 3.2 + MyBatis-Plus 的电商系统（hmall），已成功集成 Spring AI（DashScope/通义千问），并验证了 AI 对话能力（`AIController.java`）。接下来需要将 AI 从"对话"升级为"行动 Agent"，实现用户通过自然语言与系统交互，自动完成**搜索商品 → 加购 → 下单 → 支付**的全流程，并配套完善的**安全防护（接口限流、权限校验）** 体系。

---

## 一、总体架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    用户层 (User)                          │
│  用户发送自然语言消息 (WebSocket / SSE / HTTP)            │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  AI Agent 层                              │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ 意图识别     │  │ 对话管理     │  │ 工具调用      │  │
│  │ (NLU)       │→│ (Memory)     │→│ (Function     │  │
│  │             │  │              │  │  Calling)     │  │
│  └─────────────┘  └──────────────┘  └───────┬───────┘  │
└──────────────────────────────────────────────┼──────────┘
                                               │
┌──────────────────────────────────────────────▼──────────┐
│                  工具层 (Tool Layer)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │ 商品搜索  │ │ 购物车   │ │ 下单     │ │ 支付      │  │
│  │ Tool     │ │ Tool     │ │ Tool     │ │ Tool      │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬─────┘  │
└───────┼────────────┼────────────┼──────────────┼────────┘
        │            │            │              │
┌───────▼────────────▼────────────▼──────────────▼────────┐
│                  业务层 (Business Layer)                  │
│   ItemService  CartService  OrderService  PayService    │
│   UserService  AddressService  ...                      │
└─────────────────────────────────────────────────────────┘
```

---

## 二、阶段一：AI Agent 核心能力建设

### 2.1 引入 Function Calling（工具调用）

**目标**：让 AI 模型能够调用 Java 方法，实现"思考→行动"的闭环。

| 步骤 | 内容 | 技术方案 |
|------|------|----------|
| 1 | 定义 Tool 接口规范 | 使用 Spring AI 的 `@Tool` 注解或自定义 `ToolCallback` |
| 2 | 实现商品搜索 Tool | 封装 `ItemService` 的搜索逻辑 |
| 3 | 实现购物车 Tool | 封装 `CartService` 的增删查逻辑 |
| 4 | 实现下单 Tool | 封装 `OrderService.createOrder()` |
| 5 | 实现支付 Tool | 封装 `PayService.tryPayOrderByBalance()` |
| 6 | 实现用户信息 Tool | 封装 `UserService` 的登录、余额查询等 |

**关键代码示例**：

```java
// 商品搜索 Tool
@Component
public class ItemSearchTool {

    private final IItemService itemService;

    @Tool(name = "searchItems", description = "根据关键词搜索商品，返回商品列表")
    public List<ItemDTO> searchItems(@ToolParam(description = "搜索关键词") String keyword) {
        // 调用 ItemService 的搜索逻辑
        return itemService.searchByKeyword(keyword);
    }

    @Tool(name = "getItemDetail", description = "根据商品ID查询商品详情")
    public ItemDTO getItemDetail(@ToolParam(description = "商品ID") Long itemId) {
        return itemService.queryItemById(itemId);
    }
}
```

```java
// 购物车 Tool
@Component
public class CartTool {

    private final ICartService cartService;

    @Tool(name = "addToCart", description = "将商品添加到购物车")
    public void addToCart(
            @ToolParam(description = "商品ID") Long itemId,
            @ToolParam(description = "商品名称") String name,
            @ToolParam(description = "商品价格(分)") Integer price,
            @ToolParam(description = "商品图片URL") String image,
            @ToolParam(description = "商品规格") String spec) {
        CartFormDTO dto = new CartFormDTO();
        dto.setItemId(itemId);
        dto.setName(name);
        dto.setPrice(price);
        dto.setImage(image);
        dto.setSpec(spec);
        cartService.addItem2Cart(dto);
    }

    @Tool(name = "viewCart", description = "查看当前用户的购物车内容")
    public List<CartVO> viewCart() {
        return cartService.queryMyCarts();
    }

    @Tool(name = "clearCart", description = "清空购物车")
    public void clearCart() {
        // 获取当前用户购物车所有条目并删除
    }
}
```

```java
// 下单 & 支付 Tool
@Component
public class OrderTool {

    private final IOrderService orderService;
    private final IPayOrderService payOrderService;

    @Tool(name = "createOrder", description = "创建订单，传入商品详情列表和支付方式")
    public Long createOrder(
            @ToolParam(description = "商品详情列表，格式：[{itemId: 1, num: 2}]") 
            List<OrderDetailDTO> details,
            @ToolParam(description = "支付类型：3表示余额支付") Integer paymentType,
            @ToolParam(description = "收货地址ID") Long addressId) {
        OrderFormDTO form = new OrderFormDTO();
        form.setDetails(details);
        form.setPaymentType(paymentType);
        form.setAddressId(addressId);
        return orderService.createOrder(form);
    }

    @Tool(name = "payOrder", description = "使用余额支付订单")
    public void payOrder(
            @ToolParam(description = "订单ID") Long orderId,
            @ToolParam(description = "支付密码") String password) {
        // 1. 生成支付单
        PayApplyDTO applyDTO = PayApplyDTO.builder()
                .bizOrderNo(orderId)
                .amount(null) // 由系统查询
                .payChannelCode("balance")
                .payType(3) // 余额支付
                .build();
        String payOrderId = payOrderService.applyPayOrder(applyDTO);
        // 2. 执行支付
        PayOrderFormDTO payForm = PayOrderFormDTO.builder()
                .id(Long.valueOf(payOrderId))
                .pw(password)
                .build();
        payOrderService.tryPayOrderByBalance(payForm);
    }
}
```

### 2.2 构建 AI Agent 核心控制器

**目标**：创建一个统一的 Agent 入口，接收用户自然语言，调用 AI 模型进行意图识别和工具调用。

```java
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ChatModel chatModel;
    private final List<ToolCallback> toolCallbacks;

    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody AgentRequest request) {
        // 构建带有工具的系统提示词
        SystemPromptTemplate systemPrompt = new SystemPromptTemplate("""
            你是一个智能购物助手，可以帮助用户完成以下操作：
            1. 搜索商品 - 使用 searchItems 工具
            2. 查看商品详情 - 使用 getItemDetail 工具
            3. 添加商品到购物车 - 使用 addToCart 工具
            4. 查看购物车 - 使用 viewCart 工具
            5. 创建订单 - 使用 createOrder 工具
            6. 支付订单 - 使用 payOrder 工具
            
            请根据用户的自然语言指令，调用相应的工具来完成任务。
            在每次操作前，请先向用户确认操作细节。
            """);

        // 构建带有工具支持的 Prompt
        Prompt prompt = new Prompt(
            request.getMessage(),
            ChatOptions.builder()
                .withTools(toolCallbacks)
                .build()
        );

        // 流式响应
        return chatModel.stream(prompt)
            .map(response -> response.getResult().getOutput().getContent());
    }
}
```

### 2.3 对话记忆（Memory）管理

**目标**：让 Agent 记住对话上下文，实现多轮交互。

| 方案 | 说明 | 适用场景 |
|------|------|----------|
| **In-Memory** | 基于 `ChatMemory` 接口，存储在本地内存 | 开发测试、单实例部署 |
| **Redis** | 基于 Redis 存储对话历史 | 生产环境、多实例部署 |
| **数据库** | 基于 MySQL 持久化对话历史 | 需要长期保存对话记录 |

**推荐方案**：先使用 In-Memory 快速验证，后续迁移到 Redis。

```java
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是智能购物助手，帮助用户完成电商平台的购物操作。
                    你可以搜索商品、查看详情、管理购物车、下单和支付。
                    请用友好的语气与用户交流，并在执行关键操作前确认用户意图。
                    """)
                .build();
    }
}
```

---

## 三、阶段二：全自动购物流程串联

### 3.1 完整业务流程

```
用户: "我想买一台笔记本电脑"
    ↓
Agent: 调用 searchItems("笔记本电脑") → 返回商品列表
    ↓
Agent: "为您找到以下笔记本电脑：1. MacBook Pro ¥12999 ... 请问您想了解哪一款？"
    ↓
用户: "我想看看第一款"
    ↓
Agent: 调用 getItemDetail(1) → 返回商品详情
    ↓
Agent: "MacBook Pro 配置：... 价格：¥12999，库存充足。要加入购物车吗？"
    ↓
用户: "加入购物车"
    ↓
Agent: 调用 addToCart(1, "MacBook Pro", 1299900, ...) → 成功
    ↓
Agent: "已添加到购物车！当前购物车有1件商品。需要继续购物还是去结算？"
    ↓
用户: "去结算"
    ↓
Agent: 调用 viewCart() → 显示购物车内容
    ↓
Agent: "您的购物车：MacBook Pro × 1，共¥12999。请确认收货地址和支付方式。"
    ↓
用户: "确认下单，余额支付，密码是123456"
    ↓
Agent: 调用 createOrder([{itemId:1, num:1}], 3, addressId) → 返回订单ID
    ↓
Agent: 调用 payOrder(orderId, "123456") → 支付成功
    ↓
Agent: "订单已创建并支付成功！订单号：xxx，预计3-5天送达。"
```

### 3.2 状态机与流程控制

引入**有限状态机**来管理 Agent 的对话流程状态：

```
状态: IDLE → SEARCHING → VIEWING → CARTING → ORDERING → PAYING → DONE
                    ↑          ↑         ↑          ↑         ↑
                    └──────────┴─────────┴──────────┴─────────┘ (可回退)
```

```java
public enum AgentState {
    IDLE,           // 空闲/初始状态
    SEARCHING,      // 搜索商品中
    VIEWING,        // 查看商品详情
    CARTING,        // 购物车操作
    ORDERING,       // 下单流程
    PAYING,         // 支付流程
    DONE            // 完成
}
```

### 3.3 用户身份管理

由于 Agent 需要代表用户执行操作，必须解决**用户身份认证**问题：

| 方案 | 说明 | 实现方式 |
|------|------|----------|
| **Token 传递** | 前端在请求 Agent 时携带 JWT Token | 从请求头提取 userId 设置到 UserContext |
| **会话绑定** | Agent 会话与用户绑定 | 每个会话创建时关联 userId |
| **临时用户** | 未登录用户只能搜索，不能下单 | 限制工具调用权限 |

**关键点**：Agent 控制器需要从请求中提取用户 Token，解析出 userId，设置到 `UserContext` 中，确保后续工具调用能正确识别用户身份。

---

## 四、阶段三：安全防护体系

### 4.1 接口限流（Rate Limiting）

**目标**：防止恶意请求刷接口，保护后端服务。

#### 方案一：基于 Redis + Lua 的滑动窗口限流（推荐）

```java
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 滑动窗口限流
     * @param key 限流key (如 "rate_limit:user:123")
     * @param maxRequests 窗口内最大请求数
     * @param windowSeconds 窗口大小（秒）
     * @return true=允许通过, false=被限流
     */
    public boolean allowRequest(String key, int maxRequests, long windowSeconds) {
        String luaScript = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local max = tonumber(ARGV[3])
            
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)
            local count = redis.call('ZCARD', key)
            
            if count >= max then
                return 0
            end
            
            redis.call('ZADD', key, now, now .. ':' .. math.random())
            redis.call('EXPIRE', key, window)
            return 1
            """;
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            List.of(key),
            System.currentTimeMillis(),
            windowSeconds,
            maxRequests
        );
        return result != null && result == 1L;
    }
}
```

#### 方案二：基于注解的声明式限流

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";
    int maxRequests() default 10;     // 最大请求数
    long windowSeconds() default 1;   // 时间窗口（秒）
    String message() default "请求过于频繁，请稍后再试";
}
```

```java
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建限流key：用户ID + 接口名
        Long userId = UserContext.getUser();
        String key = "rate_limit:" + userId + ":" + rateLimit.key();

        if (!rateLimiter.allowRequest(key, rateLimit.maxRequests(), rateLimit.windowSeconds())) {
            throw new RuntimeException(rateLimit.message());
        }
        return joinPoint.proceed();
    }
}
```

#### 限流策略配置

| 接口 | 限流策略 | 说明 |
|------|----------|------|
| `/agent/chat` | 10次/分钟/用户 | AI 对话接口，消耗 Token 成本高 |
| `/orders` | 5次/分钟/用户 | 下单接口，防止刷单 |
| `/pay-orders` | 3次/分钟/用户 | 支付接口，资金安全 |
| `/carts` | 20次/分钟/用户 | 购物车操作 |
| `/items/**` | 30次/分钟/用户 | 商品查询，频率可放宽 |
| `/users/login` | 5次/分钟/IP | 登录接口，防暴力破解 |

### 4.2 接口防刷与幂等性

| 措施 | 实现方式 | 说明 |
|------|----------|------|
| **Token 令牌** | 前端请求先获取 token，后端校验 | 防止直接调用接口 |
| **幂等性 Key** | 基于 Redis 的幂等性校验 | 防止重复下单/支付 |
| **签名校验** | 请求参数 + 时间戳 + 密钥签名 | 防止请求被篡改 |
| **验证码** | 图形验证码 / 短信验证码 | 关键操作二次确认 |

### 4.3 安全校验清单

```java
// Agent 请求安全校验拦截器
@Component
public class AgentSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        // 1. Token 校验 - 解析用户身份
        String token = request.getHeader("Authorization");
        Long userId = jwtTool.parseToken(token);
        UserContext.setUser(userId);

        // 2. 接口限流校验
        if (!rateLimiter.allowRequest("agent:" + userId, 10, 60)) {
            throw new RateLimitException("请求过于频繁");
        }

        // 3. 操作频率校验（防止 AI 自动高频调用）
        // ...

        // 4. 敏感操作二次确认（支付金额超过阈值）
        // ...

        return true;
    }
}
```

### 4.4 敏感操作确认机制

对于**下单、支付**等敏感操作，Agent 不能直接执行，需要先向用户确认：

```
用户: "直接帮我下单付款"
Agent: "⚠️ 即将为您下单以下商品：
       - MacBook Pro × 1 = ¥12,999
       - 收货地址：北京市朝阳区...
       - 支付方式：余额支付
       请确认是否继续？(回复"确认"或"取消")"
用户: "确认"
Agent: → 执行下单 + 支付
```

---

## 五、阶段四：增强与优化

### 5.1 多模型支持

| 模型 | 集成方式 | 适用场景 |
|------|----------|----------|
| **通义千问 (DashScope)** | 已集成 | 默认模型，国内访问稳定 |
| **DeepSeek** | DashScope 已支持 | 推理能力强，适合复杂任务 |
| **OpenAI** | 引入 `spring-ai-starter-model-openai` | 备选方案 |

### 5.2 异常处理与回退

```java
@ControllerAdvice
public class AgentExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<AgentResponse> handleRateLimit(RateLimitException e) {
        return ResponseEntity.status(429)
            .body(AgentResponse.error("请求过于频繁，请稍后再试"));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<AgentResponse> handlePayment(PaymentException e) {
        return ResponseEntity.ok(
            AgentResponse.error("支付失败：" + e.getMessage())
                .withSuggestions(List.of("检查支付密码", "检查余额是否充足"))
        );
    }

    @ExceptionHandler(StockException.class)
    public ResponseEntity<AgentResponse> handleStock(StockException e) {
        return ResponseEntity.ok(
            AgentResponse.error("库存不足：" + e.getMessage())
                .withSuggestions(List.of("查看其他相似商品"))
        );
    }
}
```

### 5.3 日志与监控

| 监控项 | 实现方式 | 目的 |
|--------|----------|------|
| **Agent 调用日志** | AOP 切面记录每次工具调用 | 审计、调试 |
| **Token 消耗统计** | 记录每次 AI 调用的 Token 数 | 成本控制 |
| **操作链路追踪** | MDC + TraceId | 全链路排查问题 |
| **限流告警** | 限流触发时记录日志 + 告警 | 安全监控 |

```java
@Aspect
@Component
@Slf4j
public class AgentLogAspect {

    @Around("@annotation(tool)")
    public Object logToolCall(ProceedingJoinPoint pjp, Tool tool) throws Throwable {
        String toolName = tool.name();
        Object[] args = pjp.getArgs();
        Long userId = UserContext.getUser();
        
        log.info("[Agent] 用户: {}, 调用工具: {}, 参数: {}", userId, toolName, args);
        
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[Agent] 工具: {} 执行成功, 耗时: {}ms, 结果: {}", toolName, cost, result);
            return result;
        } catch (Exception e) {
            log.error("[Agent] 工具: {} 执行失败, 耗时: {}ms, 错误: {}", 
                toolName, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
```

---

## 六、实施路线图

### 第一阶段：基础 Agent 能力（1-2天）
- [ ] 定义 Tool 接口规范，实现 `ToolCallback`
- [ ] 实现商品搜索、购物车、下单、支付等核心 Tool
- [ ] 构建 Agent 控制器，集成 Function Calling
- [ ] 验证端到端流程：搜索 → 加购 → 下单 → 支付

### 第二阶段：安全防护体系（1天）
- [ ] 集成 Redis，实现滑动窗口限流
- [ ] 实现注解式限流 `@RateLimit`
- [ ] 为所有关键接口配置限流策略
- [ ] 实现敏感操作二次确认机制

### 第三阶段：对话管理与体验优化（1天）
- [ ] 集成对话记忆（ChatMemory）
- [ ] 实现多轮对话上下文管理
- [ ] 优化 Agent 提示词（Prompt Engineering）
- [ ] 实现异常处理与用户友好提示

### 第四阶段：生产化增强（1-2天）
- [ ] 日志与监控体系
- [ ] Token 消耗统计与成本控制
- [ ] 多模型切换支持
- [ ] 压力测试与性能优化

---

## 七、技术依赖清单

| 依赖 | 用途 | 是否已有 |
|------|------|----------|
| `spring-ai-starter-model-openai` | OpenAI 模型支持 | ✅ 已引入 |
| `spring-ai-alibaba-starter-dashscope` | 通义千问模型支持 | ✅ 已引入 |
| `spring-boot-starter-data-redis` | Redis 限流/缓存/会话 | ❌ 需引入 |
| `spring-boot-starter-aop` | AOP 切面（限流/日志） | ✅ Spring Boot 自带 |
| `redisson` | 分布式锁/限流高级功能 | ❌ 可选引入 |

---

## 八、关键注意事项

### 8.1 安全红线
1. **支付密码不能明文传输**：前端加密，后端解密
2. **敏感操作必须用户确认**：Agent 不能自动执行下单/支付
3. **限流是最后防线**：业务层面也要做防刷设计
4. **Token 安全**：Agent 接口必须校验 JWT Token

### 8.2 成本控制
1. AI 模型调用按 Token 计费，需控制每次对话的 Token 消耗
2. 设置用户级别的每日 Token 上限
3. 使用流式响应（SSE/Flux）提升用户体验，减少等待

### 8.3 用户体验
1. Agent 回复要简洁明了，避免过长回复
2. 关键操作前必须向用户确认
3. 提供清晰的错误提示和解决方案建议
4. 支持 WebSocket 实时推送，提升交互流畅度

---

## 九、项目文件结构规划

```
hm-service/src/main/java/com/hmall/
├── agent/                          # AI Agent 相关代码（新建）
│   ├── AgentController.java        # Agent 统一入口
│   ├── AgentConfig.java            # Agent 配置
│   ├── AgentState.java             # 对话状态枚举
│   ├── tool/                       # 工具定义（新建）
│   │   ├── ItemSearchTool.java     # 商品搜索工具
│   │   ├── CartTool.java           # 购物车工具
│   │   ├── OrderTool.java          # 下单工具
│   │   └── PayTool.java            # 支付工具
│   └── dto/                        # Agent DTO（新建）
│       ├── AgentRequest.java
│       └── AgentResponse.java
├── config/
│   ├── RateLimitConfig.java        # 限流配置（新建）
│   └── RedisConfig.java            # Redis 配置（新建）
├── interceptor/
│   ├── LoginInterceptor.java       # 已有
│   └── RateLimitInterceptor.java   # 限流拦截器（新建）
├── annotation/                     # 自定义注解（新建）
│   └── RateLimit.java              # 限流注解
├── aspect/                         # AOP 切面（新建）
│   ├── RateLimitAspect.java        # 限流切面
│   └── AgentLogAspect.java         # Agent 日志切面
└── utils/
    └── RateLimiter.java            # 限流工具类（新建）
```

---

## 十、总结

本大纲规划了从"AI 对话"到"AI Agent 全自动购物"的完整演进路径，核心要点：

1. **Function Calling** 是 AI Agent 的核心能力，让大模型能调用 Java 业务方法
2. **安全防护** 是生产环境的必备条件，限流 + 鉴权 + 敏感操作确认缺一不可
3. **分阶段实施**，先跑通核心流程，再逐步完善安全、体验、监控
4. **成本可控**，通过 Token 统计和限流策略控制 AI 调用成本

> 💡 **建议**：先以第一阶段为目标，实现"搜索→加购→下单→支付"的完整 Agent 流程，验证可行性后再逐步叠加安全防护和体验优化。
