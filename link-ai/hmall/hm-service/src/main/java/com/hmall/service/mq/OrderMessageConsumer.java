package com.hmall.service.mq;

import com.hmall.common.constants.RabbitMQConstants;
import com.hmall.common.domain.msg.CartCleanMessage;
import com.hmall.common.domain.msg.OrderStatusMessage;
import com.hmall.domain.po.Order;
import com.hmall.service.ICartService;
import com.hmall.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.hmall.common.constants.RabbitMQConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final ICartService cartService;
    private final IOrderService orderService;
    private final OrderMessageProducer orderMessageProducer;

    @RabbitListener(queues = CART_CLEAN_QUEUE)
    public void handleCartClean(CartCleanMessage message) {
        log.info("收到购物车清理消息: userId={}, itemIds={}",
                message.getUserId(), message.getItemIds());
        cartService.removeByItemIds(message.getItemIds(), message.getUserId());
        log.info("购物车清理完成: userId={}", message.getUserId());
    }

    @RabbitListener(queues = ORDER_STATUS_QUEUE)
    public void handleOrderStatus(OrderStatusMessage message) {
        log.info("收到订单状态消息: orderId={}, status={}, reason={}",
                message.getOrderId(), message.getStatus(), message.getReason());

        if (message.getStatus() == 1) {
            handleOrderTimeout(message.getOrderId());
        } else {
            handleOrderFail(message);
        }
    }

    private void handleOrderTimeout(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order != null && order.getStatus() == 1) {
            order.setStatus(5);
            orderService.updateById(order);
            log.info("订单超时取消: orderId={}", orderId);

            orderMessageProducer.sendOrderStatusMessage(
                    OrderStatusMessage.builder()
                            .orderId(orderId)
                            .status(5)
                            .reason("超时未支付已取消")
                            .build());
        }
    }

    private void handleOrderFail(OrderStatusMessage message) {
        Order order = orderService.getById(message.getOrderId());
        if (order != null) {
            order.setStatus(message.getStatus());
            orderService.updateById(order);
            log.info("订单状态更新: orderId={}, status={}",
                    message.getOrderId(), message.getStatus());
        }
    }
}