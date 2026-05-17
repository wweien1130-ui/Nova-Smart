package com.hmall.api.client;

import com.hmall.common.domain.dto.OrderFormDTO;
import com.hmall.common.domain.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务Feign客户端
 */
@FeignClient(name = "hmall-service", path = "/orders", configuration = com.hmall.api.config.DefaultFeignConfig.class)
public interface OrderFeignClient {

    @PostMapping
    Long createOrder(@RequestBody OrderFormDTO orderFormDTO);

    @GetMapping("/{id}")
    OrderVO queryOrderById(@PathVariable("id") Long orderId);

    @GetMapping
    List<OrderVO> queryMyOrders();

    @PutMapping("/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);

    @PutMapping("/{orderId}/cancel")
    void cancelOrder(@PathVariable("orderId") Long orderId);

    @PutMapping("/{orderId}/confirm")
    void confirmReceipt(@PathVariable("orderId") Long orderId);
}