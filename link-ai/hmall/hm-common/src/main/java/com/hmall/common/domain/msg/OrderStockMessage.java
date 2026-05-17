package com.hmall.common.domain.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStockMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long orderId;
    private Long userId;
    private List<OrderItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long itemId;
        private Integer num;
    }
}