package com.liang.agent.result;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.hmall.common.domain.dto.ItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemInfo {
    @JsonPropertyDescription("商品ID")
    private Long id;

    @JsonPropertyDescription("商品名称")
    private String name;

    @JsonPropertyDescription("商品价格（单位：分）")
    private double price;

    @JsonPropertyDescription("商品图片URL")
    private String image;

    @JsonPropertyDescription("商品分类")
    private String category;

    @JsonPropertyDescription("商品品牌")
    private String brand;

    @JsonPropertyDescription("商品规格")
    private String spec;


    /**
     * 将ItemDTO转换为ItemInfo
     * **/
    public  static ItemInfo of(ItemDTO itemDTO) {
        if(itemDTO == null){
            return null;
        }
        //拷贝对象(忽略异常)
        ItemInfo itemInfo = BeanUtil.toBeanIgnoreError(itemDTO, ItemInfo.class);
        //价格格式化处理：分转换为元，保留2位小数
        itemInfo.setPrice(Optional.ofNullable(itemDTO.getPrice())
                        .map(num -> num.doubleValue() / 100d)
                        .map(num -> NumberUtil.round(num, 2)).get().doubleValue());
        return itemInfo;



    }
}
