package com.hmall.common.domain.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private LocalDateTime timestamp;
}