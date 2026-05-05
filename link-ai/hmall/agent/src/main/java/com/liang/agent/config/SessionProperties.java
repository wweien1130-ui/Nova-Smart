package com.liang.agent.config;

import com.liang.agent.domain.vo.SessionVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@RefreshScope

@ConfigurationProperties(prefix = "tj.ai.session")
public class SessionProperties {

    /**
     * AI助手的标题，用于显示助手的名称或身份
     * **/
    private String title;

    /**
     * AI助手的描述，简要介绍助手的功能或特定
     * **/
    private String describe;

    /**
     * 实例列表，包含一些使用助手的示例
     * **/
    private List<SessionVO.Example> examples;
}
