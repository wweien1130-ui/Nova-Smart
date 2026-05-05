package com.liang.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.liang.agent.mapper")
@EnableFeignClients(basePackages = "com.hmall.api.client")  // 新增
@SpringBootApplication(scanBasePackages = {"com.liang.agent"},
    exclude = {org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class})
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}