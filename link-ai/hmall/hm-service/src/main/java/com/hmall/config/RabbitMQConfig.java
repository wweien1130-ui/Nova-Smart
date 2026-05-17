package com.hmall.config;

import com.hmall.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hmall.common.constants.RabbitMQConstants.*;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    // 交换机
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    // 队列
    @Bean
    public Queue stockDeductQueue() {
        return QueueBuilder.durable(STOCK_DEDUCT_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_STATUS_KEY)
                .build();
    }

    @Bean
    public Queue cartCleanQueue() {
        return QueueBuilder.durable(CART_CLEAN_QUEUE).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArgument("x-message-ttl", ORDER_DELAY_TTL)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_STATUS_KEY)
                .build();
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(ORDER_STATUS_QUEUE).build();
    }

    // 绑定关系
    @Bean
    public Binding stockDeductBinding() {
        return BindingBuilder.bind(stockDeductQueue())
                .to(orderExchange())
                .with(STOCK_DEDUCT_KEY);
    }

    @Bean
    public Binding cartCleanBinding() {
        return BindingBuilder.bind(cartCleanQueue())
                .to(orderExchange())
                .with(CART_CLEAN_KEY);
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue())
                .to(orderExchange())
                .with(ORDER_STATUS_KEY);
    }
}