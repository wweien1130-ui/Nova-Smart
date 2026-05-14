package com.hmall.common.domain;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Min;

@Data
@ApiModel(description = "分页查询条件")       // Swagger 文档展示
@Accessors(chain = true)                     // 链式调用：pageQuery.setPageNo(1).setPageSize(20)
public class PageQuery {
    public static final Integer DEFAULT_PAGE_SIZE = 20; // 默认每页20条
    public static final Integer DEFAULT_PAGE_NUM = 1;   // 默认第1页
    @ApiModelProperty("页码")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = DEFAULT_PAGE_NUM;
    @ApiModelProperty("每页查询数量")
    @Min(value = 1, message = "每页查询数量不能小于1")
    private Integer pageSize = DEFAULT_PAGE_SIZE;
    @ApiModelProperty("是否升序")
    private Boolean isAsc = true;
    @ApiModelProperty("排序方式")
    private String sortBy;

    public int from(){
        return (pageNo - 1) * pageSize;
    }

    public <T> Page<T> toMpPage(OrderItem... orderItems) {
        // 1. 创建分页对象，传入当前页和每页条数
        Page<T> page = new Page<>(pageNo, pageSize);
        // 是否手动指定排序方式
        // 2. 如果方法参数传入了排序条件（如 OrderItem.desc("id")）
        if (orderItems != null && orderItems.length > 0) {
            for (OrderItem orderItem : orderItems) {
                page.addOrder(orderItem);
            }
            return page;
        }
        // 前端是否有排序字段
        // 3. 如果没有传入排序条件，看前端是否传了 sortBy 参数
        if (StrUtil.isNotEmpty(sortBy)){
            OrderItem orderItem = new OrderItem();
            orderItem.setAsc(isAsc);      // 是否升序
            orderItem.setColumn(sortBy);   // 排序列名

            page.addOrder(orderItem);
        }
        return page;
    }

    public <T> Page<T> toMpPage(String defaultSortBy, boolean isAsc) {
        // 如果前端没传排序参数，就用默认的排序方式
        if (StringUtils.isBlank(sortBy)){
            sortBy = defaultSortBy;    // 使用传入的默认排序字段
            this.isAsc = isAsc;       // 使用传入的默认排序方向
        }
        Page<T> page = new Page<>(pageNo, pageSize);
        OrderItem orderItem = new OrderItem();
        orderItem.setAsc(this.isAsc);      // 是否升序
        orderItem.setColumn(sortBy);  // 排序列名
        page.addOrder(orderItem);
        return page;
    }
    public <T> Page<T> toMpPageDefaultSortByCreateTimeDesc() {
        return toMpPage("create_time", false);
    }
}
