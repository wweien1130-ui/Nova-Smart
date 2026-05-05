package com.hmall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.dto.OrderFormDTO;
import com.hmall.domain.po.Order;

import java.util.List;

/**
 * <p>
 * 订单服务接口
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    List<Order> queryByUserId(Long userId);

    /**
     * 取消订单（仅限未付款订单）
     * @param orderId 订单ID
     * @return 是否取消成功
     */
    boolean cancelOrder(Long orderId);

    /**
     * 确认收货（仅限已发货订单）
     * @param orderId 订单ID
     * @return 是否确认成功
     */
    boolean confirmReceipt(Long orderId);

    Order getById(Long orderId);
}
