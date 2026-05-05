package com.liang.agent.enums;

import com.hmall.common.enums.BaseEnum;
import lombok.Getter;

/**
 * 消息类型枚举
 */
public enum MessageTypeEnum implements BaseEnum<Integer> {
    USER(1, "用户提问"), ASSISTANT(2, "AI的回答");

    private final int value;
    private final String desc;

    MessageTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
