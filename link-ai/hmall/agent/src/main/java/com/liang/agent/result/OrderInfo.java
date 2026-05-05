package com.liang.agent.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfo {
    @JsonPropertyDescription("订单ID")
    private Long id;

    @JsonPropertyDescription("总金额（单位：元）")
    private double totalFee;

    @JsonPropertyDescription("支付类型：1-支付宝 2-微信 3-余额")
    private Integer paymentType;

    @JsonPropertyDescription("订单状态：1-未付款 2-已付款 3-已发货 4-确认收货 5-交易取消 6-已评价")
    private Integer status;

    @JsonPropertyDescription("订单状态描述")
    private String statusDesc;

    @JsonPropertyDescription("创建时间")
    private String createTime;
}