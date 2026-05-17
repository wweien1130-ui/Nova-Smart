package com.hmall.service.mq;

import com.hmall.common.constants.RabbitMQConstants;
import com.hmall.common.domain.dto.OrderDetailDTO;
import com.hmall.common.domain.msg.OrderStockMessage;
import com.hmall.common.domain.msg.OrderStatusMessage;
import com.hmall.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.hmall.common.constants.RabbitMQConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageConsumer {

    private final IItemService itemService;
    private final OrderMessageProducer orderMessageProducer;

    @RabbitListener(queues = STOCK_DEDUCT_QUEUE)
    public void handleStockDeduct(OrderStockMessage message) {
        try {
            log.info("收到库存扣减消息: orderId={}", message.getOrderId());

            List<OrderDetailDTO> items = message.getItems().stream()
                    .map(item -> OrderDetailDTO.builder()
                            .itemId(item.getItemId())
                            .num(item.getNum())
                            .build())
                    .toList();

            itemService.deductStock(items);
            log.info("库存扣减成功: orderId={}", message.getOrderId());

        } catch (Exception e) {
            log.error("库存扣减失败: orderId={}, error={}",
                    message.getOrderId(), e.getMessage());

            orderMessageProducer.sendOrderStatusMessage(
                    OrderStatusMessage.builder()
                            .orderId(message.getOrderId())
                            .status(5)
                            .reason("库存不足")
                            .build());
        }
    }
}