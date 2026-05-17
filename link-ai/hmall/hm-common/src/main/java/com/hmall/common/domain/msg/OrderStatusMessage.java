package com.hmall.common.domain.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long orderId;
    private Integer status;
    private String reason;
}