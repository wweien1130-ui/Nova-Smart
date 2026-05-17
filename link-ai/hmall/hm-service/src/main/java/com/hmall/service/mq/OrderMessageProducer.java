package com.hmall.service.mq;

import com.hmall.common.constants.RabbitMQConstants;
import com.hmall.common.domain.msg.CartCleanMessage;
import com.hmall.common.domain.msg.OrderStatusMessage;
import com.hmall.common.domain.msg.OrderStockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.hmall.common.constants.RabbitMQConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendStockDeductMessage(OrderStockMessage message) {
        log.info("发送库存扣减消息: orderId={}", message.getOrderId());
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, STOCK_DEDUCT_KEY, message);
    }

    public void sendCartCleanMessage(CartCleanMessage message) {
        log.info("发送购物车清理消息: userId={}, itemIds={}",
                message.getUserId(), message.getItemIds());
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, CART_CLEAN_KEY, message);
    }

    public void sendDelayOrderMessage(Long orderId) {
        log.info("发送延迟订单消息: orderId={}", orderId);
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_DELAY_QUEUE, //← 交换机 && 这里直接发到延迟队列
                OrderStatusMessage.builder()
                        .orderId(orderId)
                        .status(1)  // ← 状态=1 表示"待支付/超时检查"
                        .reason("超时未支付")
                        .build());
    }

    public void sendOrderStatusMessage(OrderStatusMessage message) {
        log.info("发送订单状态消息: orderId={}, status={}",
                message.getOrderId(), message.getStatus());
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_STATUS_KEY, message);
    }
}