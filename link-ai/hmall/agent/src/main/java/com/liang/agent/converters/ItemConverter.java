package com.liang.agent.converters;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import com.hmall.common.domain.dto.ItemDTO;
import com.liang.agent.result.ItemInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ItemConverter {

    /**
     * 将ItemDTO转换为ItemInfo
     * **/
    // ItemConverter.java 中新增
    public static ItemInfo toItemInfo(ItemDTO dto) {
        if (dto == null) return null;
        ItemInfo info = BeanUtil.toBean(dto, ItemInfo.class);
        info.setPrice(Optional.ofNullable(dto.getPrice())
                .map(p -> p.doubleValue() / 100d)
                .map(p -> NumberUtil.round(p, 2))
                .orElse(BigDecimal.valueOf(0.0))
                .doubleValue());
        return info;
    }

    /**
     * 将ItemDTO列表转换为ItemInfo列表
     * **/
    public static List<ItemInfo> toItemInfoList(List<ItemDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream()
                .map(ItemConverter::toItemInfo)
                .toList();
    }



}