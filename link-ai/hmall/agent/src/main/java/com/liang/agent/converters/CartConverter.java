package com.liang.agent.converters;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import com.hmall.common.domain.vo.CartVO;
import com.liang.agent.result.CartInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CartConverter {

    /**
     * 将CartVO转换为CartInfo
     * **/
    public static CartInfo toCartInfo(CartVO vo) {
        if (vo == null) {
            return null;
        }
        CartInfo info = BeanUtil.toBean(vo, CartInfo.class);
        info.setPrice(Optional.ofNullable(vo.getPrice())
                .map(num -> num.doubleValue() / 100d)
                .map(num -> NumberUtil.round(num, 2))
                .orElse(BigDecimal.valueOf(0.0))
                .doubleValue());
        return info;
    }

    /**
     * 将CartVO列表转换为CartInfo列表
     * **/
    public static List<CartInfo> toCartInfoList(List<CartVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return List.of();
        }
        return voList.stream()
                .map(CartConverter::toCartInfo)
                .toList();
    }
}