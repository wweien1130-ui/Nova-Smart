package com.liang.agent.config;

import com.liang.agent.tool.AddressTool;
import com.liang.agent.tool.CartTool;
import com.liang.agent.tool.ItemSearchTool;
import com.liang.agent.tool.OrderTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Router Agent 配置
 * 为每个子智能体创建独立的 ChatClient，各自注册自己的 Tool
 */
@Configuration
public class RouterAgentConfig {

    /**
     * 路由智能体 ChatClient
     * 不注册任何业务 Tool，只负责分析用户意图并路由到对应的子智能体
     */
    @Bean("routerChatClient")
    public ChatClient routerChatClient(ChatModel openapiChatModel) {
        return ChatClient.builder(openapiChatModel)
                .defaultSystem("""
                        你是一个智能路由助手。你的职责是分析用户的问题，判断应该由哪个子智能体处理。

                        可选的智能体类型：
                        - ITEM: 商品搜索、商品详情查询相关的问题
                        - CART: 购物车管理相关的问题（添加、查看、移除商品）
                        - ORDER: 订单管理相关的问题（创建、查询、取消、确认收货、下单）
                        - ADDRESS: 地址管理相关的问题（查询、添加、更新、删除地址）

                        【重要路由规则】
                        1. "下单"、"创建订单"、"提交订单"、"购买" → ORDER（订单创建）
                        2. "加购"、"加入购物车"、"添加到购物车"、"要了"、"就要这个" → CART
                        3. "搜索商品"、"查找商品"、"推荐商品" → ITEM
                        4. "查看订单"、"查询订单"、"订单状态" → ORDER
                        5. "地址"、"收货地址" → ADDRESS

                        注意：当用户意图模糊时，优先考虑更明确的意图。例如：
                        - "我要买这个，下单" 应路由到 ORDER
                        - "加到购物车然后下单" 应路由到 ORDER

                        请严格按照以下 JSON 格式返回结果，不要返回其他内容：
                        {"agentType": "ORDER", "question": "用户原始问题"}
                        """)

                .build();
    }

    /**
     * 商品搜索智能体 ChatClient
     * 注册 ItemSearchTool，负责商品搜索和详情查询
     */
    @Bean("itemChatClient")
    public ChatClient itemChatClient(
            ChatModel dashScopeChatModel,
            ChatMemory chatMemory,
            ItemSearchTool itemSearchTool) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是商品搜索专家，负责帮助用户搜索商品和查询商品详情。")
                .defaultTools(itemSearchTool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        , new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 购物车管理智能体 ChatClient
     * 注册 CartTool，负责购物车的增删查
     */
    @Bean("cartChatClient")
    public ChatClient cartChatClient(
            ChatModel dashScopeChatModel,
            ChatMemory chatMemory,
            CartTool cartTool) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是购物车管理专家，负责帮助用户管理购物车。

                        【重要规则：必须调用工具，不能自己编造】
                        1. 所有购物车操作（加购、查看、移除）都必须通过调用对应的工具完成
                        2. 绝对不能自己编造"已加入购物车"、"购物车有XX商品"等回复
                        3. 工具调用成功后才能回复用户操作成功
                        4. 如果工具返回空结果，必须如实告知用户"购物车是空的"

                        可用的工具：
                        1. addToCart(itemId, name, price, image, spec, num) - 添加商品到购物车
                           - 所有参数必须提供，不能省略
                           - 如果用户没有提供完整参数，可以从对话历史中获取（如之前搜索到的商品信息）
                           - 如果用户说"可以"、"好的"、"加购"等确认语，直接调用 addToCart 工具
                           - 不要询问用户"是否要加购"，直接执行加购操作
                           - price 单位是分，注意转换

                        2. viewCart() - 查看购物车内容
                           - 调用后必须如实展示工具返回的结果
                           - 如果返回空列表，必须告诉用户"购物车是空的"

                        3. removeFromCart(itemId) - 从购物车移除商品
                           - 先调用 viewCart() 获取购物车列表，从返回结果中获取要移除商品的 itemId
                           - 然后用获取到的 itemId 调用 removeFromCart
                           - 如果用户说"移除XX商品"、"删除XX"等，直接执行移除操作，不要询问确认

                        注意：当用户明确表示要加购或移除时，直接调用对应的工具，不要犹豫。
                        """)

                .defaultTools(cartTool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        , new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 订单管理智能体 ChatClient
     * 注册 OrderTool，负责订单的增删查改
     */
    @Bean("orderChatClient")
    public ChatClient orderChatClient(
            ChatModel dashScopeChatModel,
            ChatMemory chatMemory,
            OrderTool orderTool) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是订单管理专家，负责帮助用户管理订单，包括创建订单、查询订单、取消订单、确认收货等操作。")
                .defaultTools(orderTool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        , new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 地址管理智能体 ChatClient
     * 注册 AddressTool，负责地址的增删查改
     */
    @Bean("addressChatClient")
    public ChatClient addressChatClient(
            ChatModel dashScopeChatModel,
            ChatMemory chatMemory,
            AddressTool addressTool) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是地址管理专家，负责帮助用户管理收货地址。

                        【重要规则：必须调用工具，不能自己编造】
                        1. 所有地址操作（查询、添加、更新、删除）都必须通过调用对应的工具完成
                        2. 绝对不能自己编造"没有地址"、"地址已更新"等回复
                        3. 工具调用成功后才能回复用户操作成功
                        4. 如果工具返回空列表，必须如实告知用户"没有找到地址"

                        可用的工具：
                        1. listAddresses() - 查询当前用户的所有收货地址列表
                           - 调用后必须如实展示工具返回的结果
                           - 如果返回空列表，必须告诉用户"没有找到地址"

                        2. addAddress(contact, mobile, province, city, town, street, isDefault) - 添加新的收货地址

                        3. updateAddress(addressId, contact, mobile, province, city, town, street, isDefault) - 更新收货地址

                        4. deleteAddress(addressId) - 删除收货地址
                        """)
                .defaultTools(addressTool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        , new SimpleLoggerAdvisor()
                )
                .build();

    }

}