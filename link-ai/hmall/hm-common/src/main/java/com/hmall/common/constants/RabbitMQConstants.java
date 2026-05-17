package com.hmall.common.constants;

public class RabbitMQConstants {

    // 交换机
    public static final String ORDER_EXCHANGE = "order.exchange";

    // 队列
    public static final String STOCK_DEDUCT_QUEUE = "stock.deduct.queue";
    public static final String CART_CLEAN_QUEUE = "cart.clean.queue";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_STATUS_QUEUE = "order.status.queue";

    // Routing Key
    public static final String STOCK_DEDUCT_KEY = "stock.deduct";
    public static final String CART_CLEAN_KEY = "cart.clean";
    public static final String ORDER_STATUS_KEY = "order.status";

    // 延迟消息 TTL (毫秒)
    public static final int ORDER_DELAY_TTL = 30 * 60 * 1000;
}