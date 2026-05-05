package com.liang.agent.converters;

import cn.hutool.core.util.NumberUtil;
import com.hmall.common.domain.vo.OrderVO;
import com.hmall.common.domain.vo.OrderVO;
import com.liang.agent.result.OrderInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderConverter {

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "未付款",
            2, "已付款",
            3, "已发货",
            4, "确认收货",
            5, "交易取消",
            6, "已评价"
    );

    /*public static OrderInfo toOrderInfo(Order order) {
        if (order == null) {
            return null;
        }
        return buildOrderInfo(order.getId(), order.getTotalFee(),
                order.getPaymentType(), order.getStatus(), order.getCreateTime());
    }*/

    public static OrderInfo toOrderInfo(OrderVO orderVO) {
        if (orderVO == null) {
            return null;
        }
        return buildOrderInfo(orderVO.getId(), orderVO.getTotalFee(),
                orderVO.getPaymentType(), orderVO.getStatus(), orderVO.getCreateTime());
    }

    public static List<OrderInfo> toOrderInfoList(List<OrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream()
                .map(OrderConverter::toOrderInfo)
                .toList();
    }

    private static OrderInfo buildOrderInfo(Long id, Integer totalFee,
                                            Integer paymentType, Integer status,
                                            LocalDateTime createTime) {
        OrderInfo info = new OrderInfo();
        info.setId(id);
        info.setTotalFee(Optional.ofNullable(totalFee)
                .map(fee -> fee.doubleValue() / 100d)
                .map(fee -> NumberUtil.round(fee, 2))
                .orElse(BigDecimal.valueOf(0.0))
                .doubleValue());
        info.setPaymentType(paymentType);
        info.setStatus(status);
        info.setStatusDesc(STATUS_MAP.getOrDefault(status, "未知状态"));
        if (createTime != null) {
            info.setCreateTime(createTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return info;
    }
}