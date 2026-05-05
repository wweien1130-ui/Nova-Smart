package com.liang.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfigValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisConfigValidator.class);

    @Value("${spring.data.redis.sentinel.master-name:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.data.redis.sentinel.password:}")
    private String sentinelPassword;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Redis Sentinel Configuration Validation ===");
        log.info("Sentinel Master: {}", sentinelMaster);
        log.info("Sentinel Nodes: {}", sentinelNodes);
        log.info("Sentinel Password: {}", sentinelPassword != null ? "***" : "null");
        // 检查关键配置

        if (sentinelMaster == null || sentinelMaster.isEmpty()) {
            log.warn("Redis sentinel master name is null or empty!");
        }
        if (sentinelNodes == null || sentinelNodes.isEmpty()) {
            log.warn("Redis sentinel nodes is null or empty!");
        }

        log.info("=== Configuration Validation Complete ===");
    }
}