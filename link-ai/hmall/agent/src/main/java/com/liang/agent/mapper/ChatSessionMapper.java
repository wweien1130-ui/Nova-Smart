package com.liang.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liang.agent.domain.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession > {
}
