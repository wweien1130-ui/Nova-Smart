package com.tianji.es.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/check")
@RequiredArgsConstructor
public class EsHealthController {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 检测 Elasticsearch 连接状态
     * GET /check/es
     */
    @GetMapping("/es")
    public Map<String, Object> checkEs() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "Elasticsearch");
        result.put("host", "192.168.115.128:19200");

        try {
            // 获取 ES 集群健康状态
            HealthResponse health = elasticsearchClient.cluster().health();
            result.put("status", "✅ 连接成功");
            result.put("clusterName", health.clusterName());
            result.put("statusColor", health.status().jsonValue());
            result.put("numberOfNodes", health.numberOfNodes());
            result.put("activeShards", health.activeShards());
        } catch (Exception e) {
            result.put("status", "❌ 连接失败");
            result.put("error", e.getMessage());
        }

        return result;
    }
}
