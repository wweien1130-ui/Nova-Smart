package com.liang.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.sentinel.master:mymaster}")
    private String sentinelMaster;

    @Value("${spring.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.redis.sentinel.password:}")
    private String sentinelPassword;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.timeout:5000}")
    private Integer timeout;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // 检查 sentinel 配置是否有效
        if (sentinelNodes == null || sentinelNodes.isEmpty()) {
            throw new IllegalArgumentException("Redis sentinel nodes cannot be null or empty");
        }

        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(sentinelMaster);

        // 解析 sentinel nodes
        String[] nodes = sentinelNodes.split(",");
        // 添加哨兵节点
        for (String node : nodes) {
            String[] parts = node.trim().split(":");
            if (parts.length == 2) {
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            }
        }

        if (sentinelPassword != null && !sentinelPassword.isEmpty()) {
            sentinelConfig.setPassword(RedisPassword.of(sentinelPassword));
        }

        if (redisPassword != null && !redisPassword.isEmpty()) {
            sentinelConfig.setPassword(RedisPassword.of(redisPassword));
        }

        return new LettuceConnectionFactory(sentinelConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        // 专门用于字符串操作的模板

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        // 通用的 Redis 操作模板

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
