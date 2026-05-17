package com.hmall.common.domain.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCleanMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private Set<Long> itemIds;
}