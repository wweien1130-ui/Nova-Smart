package com.hmall.controller;

import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.common.domain.dto.OrderFormDTO;
import com.hmall.domain.po.Order;
import com.hmall.common.domain.vo.OrderVO;
import com.hmall.service.IOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "订单管理接口")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderService orderService;

    @ApiOperation("根据id查询订单")
    @GetMapping("{id}")
    public OrderVO queryOrderById(@Param ("订单id")@PathVariable("id") Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null || !order.getUserId().equals(UserContext.getUser())) {
            throw new BadRequestException("订单不属于当前用户或不存在");
        }
        return BeanUtils.copyBean(order, OrderVO.class);
    }

    @ApiOperation("创建订单")
    @PostMapping
    public Long createOrder(@RequestBody OrderFormDTO orderFormDTO){
        return orderService.createOrder(orderFormDTO);
    }

    @ApiOperation("标记订单已支付")
    @ApiImplicitParam(name = "orderId", value = "订单id", paramType = "path")
    @PutMapping("/{orderId}")
    public void markOrderPaySuccess(@PathVariable("orderId") Long orderId) {
        orderService.markOrderPaySuccess(orderId);
    }

    @ApiOperation("查询当前用户订单列表")
    @GetMapping
    public List<OrderVO> queryMyOrders() {
        return BeanUtils.copyList(
                orderService.queryByUserId(UserContext.getUser()),
                OrderVO.class
        );
    }

    @ApiOperation("取消订单（仅限未付款订单）")
    @PutMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable("orderId") Long orderId) {
        boolean cancelled = orderService.cancelOrder(orderId);
        if (!cancelled) {
            throw new BadRequestException("取消订单失败，可能订单状态不允许取消");
        }
    }

    @ApiOperation("确认收货（仅限已发货订单）")
    @PutMapping("/{orderId}/confirm")
    public void confirmReceipt(@PathVariable("orderId") Long orderId) {
        boolean confirmed = orderService.confirmReceipt(orderId);
        if (!confirmed) {
            throw new BadRequestException("确认收货失败，可能订单状态不允许确认收货");
        }
    }
}
