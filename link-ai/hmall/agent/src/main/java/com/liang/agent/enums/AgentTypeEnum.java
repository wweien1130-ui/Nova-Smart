package com.liang.agent.enums;

import cn.hutool.core.util.EnumUtil;
import lombok.Getter;

/**
 * 智能体类型
 */
@Getter
public enum AgentTypeEnum {
    ROUTE("ROUTE", "路由智能体"),
    ITEM("ITEM", "商品搜索智能体"),
    CART("CART", "购物车管理智能体"),
    ORDER("ORDER", "订单管理智能体"),
    ADDRESS("ADDRESS", "地址管理智能体");

    private final String agentName;
    private final String desc;

    AgentTypeEnum(String agentName, String desc) {
        this.agentName = agentName;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name();
    }


    /**
     * 通过智能体的名称查找枚举
     */
    public static AgentTypeEnum agentNameOf(String agentName) {
        return EnumUtil.getBy(AgentTypeEnum::getAgentName, agentName);
    }

}
