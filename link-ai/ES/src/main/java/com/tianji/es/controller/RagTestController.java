package com.tianji.es.controller;

import com.tianji.es.document.ItemDocument;
import com.tianji.es.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagTestController {

    private final VectorSearchService vectorSearchService;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 测试1: 纯向量搜索
     * GET /rag/vector?q=拉杆箱&topK=5
     */
    @GetMapping("/vector")
    public Map<String, Object> testVectorSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "向量搜索 (kNN)");

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.vectorSearch(query, topK);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    /**
     * 测试2: 混合搜索（向量 + 关键词）
     * GET /rag/hybrid?q=拉杆箱&topK=5
     */
    @GetMapping("/hybrid")
    public Map<String, Object> testHybridSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "混合搜索 (向量 + 关键词)");

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.hybridSearch(query, topK);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    /**
     * 测试3: 完整 RAG（检索 + LLM 生成）
     * GET /rag/chat?q=帮我推荐一款拉杆箱
     */
    @GetMapping("/chat")
    public Map<String, Object> testRagChat(@RequestParam("q") String query) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);

        long start = System.currentTimeMillis();

        // 1. 检索相关商品
        List<ItemDocument> items = vectorSearchService.hybridSearch(query, 5);
        long searchCost = System.currentTimeMillis() - start;

        // 2. 构建上下文
        String context = items.stream()
                .map(item -> String.format("【商品】%s | 品牌:%s | 分类:%s | 价格:%.2f元 | 规格:%s",
                        item.getName(),
                        item.getBrand(),
                        item.getCategory(),
                        item.getPrice() / 100.0,
                        item.getSpec() != null ? item.getSpec() : "无"))
                .collect(Collectors.joining("\n"));

        // 3. 调用大模型生成回答
        ChatClient chatClient = chatClientBuilder.build();
        String answer = chatClient.prompt()
                .system("你是一个智能购物助手。请根据以下商品信息，为用户推荐合适的商品。" +
                        "回答要简洁、友好，突出商品的特点和价格。")
                .user("用户问题：" + query + "\n\n相关商品信息：\n" + context)
                .call()
                .content();

        long totalCost = System.currentTimeMillis() - start;

        result.put("searchTimeMs", searchCost);
        result.put("totalTimeMs", totalCost);
        result.put("retrievedItems", items.size());
        result.put("context", context);
        result.put("answer", answer);

        return result;
    }

    /**
     * 纯 ES 分词搜索（毫秒级，不调 LLM）
     * GET /rag/search?q=好喝的健康的牛奶&topK=5
     *
     * ES 的 IK 分词器会自动把"好喝的健康的牛奶"拆成"好喝"、"健康"、"牛奶"来匹配
     * 适用于 agent 快速搜索场景
     */
    @GetMapping("/search")
    public Map<String, Object> testSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "ES 分词搜索（纯 match，不调 LLM）");

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.search(query, topK);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    // ==================== 带元数据过滤的搜索 API ====================


    /**
     * 测试4: 带分类/品牌过滤的向量搜索
     * GET /rag/vector/filter?q=衣服&category=服装&topK=5
     * GET /rag/vector/filter?q=手机&brand=华为&topK=5
     * GET /rag/vector/filter?q=手机&category=手机&brand=华为&topK=5
     */
    @GetMapping("/vector/filter")
    public Map<String, Object> testVectorSearchWithFilter(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "brand", required = false) String brand) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "向量搜索 + 元数据过滤");
        result.put("filter", Map.of(
                "category", category != null ? category : "不限",
                "brand", brand != null ? brand : "不限"
        ));

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.vectorSearch(query, topK, category, brand);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    /**
     * 测试5: 带分类/品牌过滤的混合搜索
     * GET /rag/hybrid/filter?q=衣服&category=服装&topK=5
     * GET /rag/hybrid/filter?q=手机&brand=华为&topK=5
     */
    @GetMapping("/hybrid/filter")
    public Map<String, Object> testHybridSearchWithFilter(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "brand", required = false) String brand) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "混合搜索 + 元数据过滤");
        result.put("filter", Map.of(
                "category", category != null ? category : "不限",
                "brand", brand != null ? brand : "不限"
        ));

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.hybridSearch(query, topK, category, brand);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    /**
     * 测试6: 智能搜索（自动推断分类/品牌）
     * GET /rag/smart?q=衣服&topK=5   → 自动推断 category=服装
     * GET /rag/smart?q=华为手机&topK=5 → 自动推断 category=手机, brand=华为
     * GET /rag/smart?q=耐克运动鞋&topK=5 → 自动推断 brand=耐克, category=鞋类
     */
    @GetMapping("/smart")
    public Map<String, Object> testSmartSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "智能搜索（自动推断元数据过滤）");

        long start = System.currentTimeMillis();
        List<ItemDocument> items = vectorSearchService.smartSearch(query, topK);
        long cost = System.currentTimeMillis() - start;

        result.put("totalHits", items.size());
        result.put("timeCostMs", cost);

        List<Map<String, Object>> itemList = items.stream().map(this::toItemMap).collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    /**
     * 测试7: 智能 RAG（自动推断 + LLM 生成）
     * GET /rag/smart/chat?q=帮我推荐一款华为手机
     * GET /rag/smart/chat?q=我想买件衣服
     */
    @GetMapping("/smart/chat")
    public Map<String, Object> testSmartRagChat(@RequestParam("q") String query) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("searchType", "智能 RAG（自动推断元数据过滤 + LLM 生成）");

        long start = System.currentTimeMillis();

        // 1. 使用智能搜索检索（自动推断 category/brand）
        List<ItemDocument> items = vectorSearchService.smartSearch(query, 5);
        long searchCost = System.currentTimeMillis() - start;

        // 2. 构建上下文
        String context = items.stream()
                .map(item -> String.format("【商品】%s | 品牌:%s | 分类:%s | 价格:%.2f元 | 规格:%s",
                        item.getName(),
                        item.getBrand(),
                        item.getCategory(),
                        item.getPrice() / 100.0,
                        item.getSpec() != null ? item.getSpec() : "无"))
                .collect(Collectors.joining("\n"));

        // 3. 调用大模型生成回答
        ChatClient chatClient = chatClientBuilder.build();
        String answer = chatClient.prompt()
                .system("你是一个智能购物助手。请根据以下商品信息，为用户推荐合适的商品。" +
                        "回答要简洁、友好，突出商品的特点和价格。")
                .user("用户问题：" + query + "\n\n相关商品信息：\n" + context)
                .call()
                .content();

        long totalCost = System.currentTimeMillis() - start;

        result.put("searchTimeMs", searchCost);
        result.put("totalTimeMs", totalCost);
        result.put("retrievedItems", items.size());
        result.put("context", context);
        result.put("answer", answer);

        return result;
    }


    /**
     * 将 ItemDocument 转为 Map（避免返回 embedding 向量）
     */
    private Map<String, Object> toItemMap(ItemDocument item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("price", item.getPrice() != null ? String.format("%.2f", item.getPrice() / 100.0) : null);
        map.put("category", item.getCategory());
        map.put("brand", item.getBrand());
        map.put("spec", item.getSpec());
        map.put("image", item.getImage());
        return map;
    }
}
