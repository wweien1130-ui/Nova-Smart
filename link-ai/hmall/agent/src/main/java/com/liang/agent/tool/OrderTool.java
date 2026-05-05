package com.liang.agent.tool;

import com.hmall.api.client.CartFeignClient;
import com.hmall.api.client.OrderFeignClient;
import com.hmall.common.domain.dto.OrderDetailDTO;
import com.hmall.common.domain.dto.OrderFormDTO;
import com.hmall.common.domain.vo.CartVO;
import com.hmall.common.domain.vo.OrderVO;
import com.liang.agent.constants.Constant;
import com.liang.agent.converters.OrderConverter;
import com.liang.agent.result.OrderInfo;
import com.liang.agent.util.ToolContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.liang.agent.constants.Constant.Tools.*;
import static com.liang.agent.constants.Constant.ToolParamns.*;

@Component
@RequiredArgsConstructor
public class OrderTool {

    private final OrderFeignClient orderFeignClient;
    private final CartFeignClient cartFeignClient;

    @Tool(name = CREATE_ORDER, description = "创建订单，将购物车中的商品下单")
    public OrderInfo createOrder(
            @ToolParam(description = ADDRESS_ID_FOR_ORDER) Long addressId,
            @ToolParam(description = PAYMENT_TYPE) Integer paymentType,
            @ToolParam(description = "是否购买购物车中的所有商品，true-全部购买，false-只购买选中的商品") Boolean buyAll,
            ToolContext toolContext) {

        System.out.println("========== 调用了 createOrder，地址ID: " + addressId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        List<CartVO> cartItems = cartFeignClient.queryMyCarts();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空，无法创建订单");
        }

        List<OrderDetailDTO> details = new ArrayList<>();
        if (Boolean.TRUE.equals(buyAll)) {
            cartItems.forEach(cart -> details.add(OrderDetailDTO.builder()
                    .itemId(cart.getItemId())
                    .num(cart.getNum())
                    .build()));
        } else {
            CartVO firstItem = cartItems.get(0);
            details.add(OrderDetailDTO.builder()
                    .itemId(firstItem.getItemId())
                    .num(firstItem.getNum())
                    .build());
        }

        OrderFormDTO orderFormDTO = new OrderFormDTO();
        orderFormDTO.setAddressId(addressId);
        orderFormDTO.setPaymentType(paymentType != null ? paymentType : 3);
        orderFormDTO.setDetails(details);

        Long orderId = orderFeignClient.createOrder(orderFormDTO);
        OrderVO orderVO = orderFeignClient.queryOrderById(orderId);

        List<Long> orderedItemIds = details.stream()
                .map(OrderDetailDTO::getItemId)
                .collect(Collectors.toList());
        cartFeignClient.deleteCartItemByIds(orderedItemIds);

        return OrderConverter.toOrderInfo(orderVO);
    }

    @Tool(name = QUERY_ORDER, description = "根据订单ID查询订单详情")
    public OrderInfo queryOrder(
            @ToolParam(description = ORDER_ID) Long orderId,
            ToolContext toolContext) {
        System.out.println("========== 调用了 queryOrder，订单ID: " + orderId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        OrderVO orderVO = orderFeignClient.queryOrderById(orderId);
        return OrderConverter.toOrderInfo(orderVO);
    }

    @Tool(name = QUERY_ORDERS, description = "查询当前用户的所有订单")
    public List<OrderInfo> queryOrders(ToolContext toolContext) {
        System.out.println("========== 调用了 queryOrders ==========");

        ToolContextUtil.setUserContext(toolContext);

        return OrderConverter.toOrderInfoList(orderFeignClient.queryMyOrders());
    }

    @Tool(name = CANCEL_ORDER, description = "取消订单（仅限未付款订单）")
    public void cancelOrder(
            @ToolParam(description = ORDER_ID) Long orderId,
            ToolContext toolContext) {
        System.out.println("========== 调用了 cancelOrder，订单ID: " + orderId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        orderFeignClient.cancelOrder(orderId);
    }

    @Tool(name = CONFIRM_RECEIPT, description = "确认收货（仅限已发货订单）")
    public void confirmReceipt(
            @ToolParam(description = ORDER_ID) Long orderId,
            ToolContext toolContext) {
        System.out.println("========== 调用了 confirmReceipt，订单ID: " + orderId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        orderFeignClient.confirmReceipt(orderId);
    }
}