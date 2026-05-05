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
public class CartInfo {
    @JsonPropertyDescription("购物车条目ID")
    private Long id;

    @JsonPropertyDescription("商品ID")
    private Long itemId;

    @JsonPropertyDescription("商品名称")
    private String name;

    @JsonPropertyDescription("购买数量")
    private Integer num;

    @JsonPropertyDescription("商品价格（单位：元）")
    private double price;

    @JsonPropertyDescription("商品图片URL")
    private String image;

    @JsonPropertyDescription("商品规格")
    private String spec;
}
