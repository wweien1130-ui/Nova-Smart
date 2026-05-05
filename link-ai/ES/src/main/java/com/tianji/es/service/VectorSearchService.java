package com.tianji.es.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.es.document.ItemDocument;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final EmbeddingModel embeddingModel;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestHighLevelClient esClient;

    public VectorSearchService(EmbeddingModel embeddingModel, ChatClient.Builder chatClientBuilder) {
        this.embeddingModel = embeddingModel;
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        this.esClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.115.128", 19200, "http"))
        );
        log.info("ES RestHighLevelClient 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (esClient != null) {
            try {
                esClient.close();
            } catch (Exception e) {
                log.error("关闭 ES 客户端失败", e);
            }
        }
    }

    // ==================== 原始搜索方法（无过滤，保留向后兼容） ====================

    /**
     * 向量搜索（无过滤）
     */
    public List<ItemDocument> vectorSearch(String query, int topK) {
        return vectorSearch(query, topK, null, null);
    }

    /**
     * 混合搜索（无过滤）
     */
    public List<ItemDocument> hybridSearch(String query, int topK) {
        return hybridSearch(query, topK, null, null);
    }

    // ==================== 带元数据过滤的搜索方法 ====================

    /**
     * 向量搜索（支持 category/brand 过滤）
     *
     * @param query    搜索关键词
     * @param topK     返回条数
     * @param category 分类过滤（null 表示不限）
     * @param brand    品牌过滤（null 表示不限）
     */
    public List<ItemDocument> vectorSearch(String query, int topK, String category, String brand) {
        try {
            float[] queryVector = generateEmbedding(query);
            log.info("生成向量完成，维度: {}", queryVector.length);

            SearchRequest searchRequest = new SearchRequest("items_vector");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 构建过滤条件
            BoolQueryBuilder filterBuilder = buildFilter(category, brand);

            // script_score 查询 + filter
            sourceBuilder.query(QueryBuilders.boolQuery()
                    .filter(filterBuilder)
                    .must(QueryBuilders.scriptScoreQuery(
                            QueryBuilders.matchAllQuery(),
                            new Script(ScriptType.INLINE, "painless",
                                    "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                                    Collections.singletonMap("queryVector", queryVector))
                    ))
            );
            sourceBuilder.size(topK);

            searchRequest.source(sourceBuilder);
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            return parseResults(response);

        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 混合搜索（支持 category/brand 过滤）
     *
     * @param query    搜索关键词
     * @param topK     返回条数
     * @param category 分类过滤（null 表示不限）
     * @param brand    品牌过滤（null 表示不限）
     */
    public List<ItemDocument> hybridSearch(String query, int topK, String category, String brand) {
        try {
            float[] queryVector = generateEmbedding(query);

            SearchRequest searchRequest = new SearchRequest("items_vector");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 构建过滤条件
            BoolQueryBuilder filterBuilder = buildFilter(category, brand);

            // bool 查询：关键词 + 向量 + filter
            sourceBuilder.query(QueryBuilders.boolQuery()
                    .filter(filterBuilder)
                    .should(QueryBuilders.matchQuery("name", query).boost(2.0f))
                    .should(QueryBuilders.matchQuery("spec", query).boost(1.0f))
                    .should(QueryBuilders.scriptScoreQuery(
                            QueryBuilders.matchAllQuery(),
                            new Script(ScriptType.INLINE, "painless",
                                    "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                                    Collections.singletonMap("queryVector", queryVector))
                    ).boost(3.0f))
                    // 至少匹配一个 should 条件
                    .minimumShouldMatch(1)
            );
            sourceBuilder.size(topK);

            searchRequest.source(sourceBuilder);
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            return parseResults(response);

        } catch (Exception e) {
            log.error("混合搜索失败", e);
            throw new RuntimeException("混合搜索失败: " + e.getMessage(), e);
        }
    }

    // ==================== 纯 ES 分词搜索（毫秒级，不调 LLM） ====================

    /**
     * 纯 ES 分词搜索：只用 ES 的 match 查询，不调 LLM 生成向量
     *
     * 适用于 agent 的快速搜索场景，毫秒级返回
     * ES 的 IK 分词器会自动把"好喝的健康的牛奶"拆成"好喝"、"健康"、"牛奶"来匹配
     *
     * @param query 搜索关键词
     * @param topK  返回条数
     */
    public List<ItemDocument> search(String query, int topK) {
        try {
            SearchRequest searchRequest = new SearchRequest("items_vector");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 纯 match 查询，利用 ES 的 IK 分词器自动分词
            sourceBuilder.query(QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("name", query).boost(3.0f))
                    .should(QueryBuilders.matchQuery("category", query).boost(2.0f))
                    .should(QueryBuilders.matchQuery("brand", query).boost(2.0f))
                    .should(QueryBuilders.matchQuery("spec", query).boost(1.0f))
                    .minimumShouldMatch(1)
            );
            sourceBuilder.size(topK);

            searchRequest.source(sourceBuilder);
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            return parseResults(response);

        } catch (Exception e) {
            log.error("ES 分词搜索失败", e);
            throw new RuntimeException("ES 分词搜索失败: " + e.getMessage(), e);
        }
    }

    // ==================== 智能搜索（关键词匹配 + LLM 兜底） ====================


    /**
     * 智能搜索：关键词快速匹配 + LLM 兜底的双层策略
     *
     * 第一层（快速通道）：关键词匹配，毫秒级响应
     *   - 从查询词中直接提取已知的分类和品牌关键词
     *   - 覆盖 90% 的常见搜索场景
     *
     * 第二层（兜底）：LLM 语义分析
     *   - 关键词匹配不到时，调 LLM 做语义理解
     *   - 适用于复杂查询（如"帮我推荐一款适合送长辈的手机"）
     *
     * 设计思路：
     * 1. MySQL 和 ES 中已有完整的 category/brand 数据（10个分类，689个品牌）
     * 2. 启动时一次性加载到内存缓存，避免每次搜索都全量扫描 ES
     * 3. 缓存定时刷新（默认5分钟），新增数据自动感知
     */
    public List<ItemDocument> smartSearch(String query, int topK) {
        long start = System.currentTimeMillis();

        // 1. 从缓存获取分类/品牌列表
        Set<String> allCategories = getCachedCategories();
        Set<String> allBrands = getCachedBrands();

        // 2. 第一层：关键词快速匹配（毫秒级）
        SearchIntent intent = matchByKeywords(query, allCategories, allBrands);
        boolean usedLLM = false;

        // 3. 关键词没匹配到，走第二层：LLM 兜底
        if (intent.category == null && intent.brand == null) {
            usedLLM = true;
            intent = analyzeIntentWithLLM(query, allCategories, allBrands);
        }

        long cost = System.currentTimeMillis() - start;
        log.info("智能搜索: query={}, category={}, brand={}, usedLLM={}, cost={}ms",
                query, intent.category, intent.brand, usedLLM, cost);

        // 4. 如果有明确的分类/品牌推断，使用带过滤的混合搜索
        if (intent.category != null || intent.brand != null) {
            return hybridSearch(query, topK, intent.category, intent.brand);
        }

        // 5. 无法推断时，回退到普通混合搜索
        return hybridSearch(query, topK, null, null);
    }


    // ==================== 缓存管理 ====================

    /** 分类缓存 */
    private volatile Set<String> cachedCategories = Collections.emptySet();
    /** 品牌缓存 */
    private volatile Set<String> cachedBrands = Collections.emptySet();
    /** 上次刷新时间 */
    private volatile long lastRefreshTime = 0;
    /** 缓存有效期（毫秒） */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟

    /**
     * 获取缓存的分类列表（懒加载 + 定时刷新）
     */
    private Set<String> getCachedCategories() {
        refreshCacheIfExpired();
        return cachedCategories;
    }

    /**
     * 获取缓存的品牌列表（懒加载 + 定时刷新）
     */
    private Set<String> getCachedBrands() {
        refreshCacheIfExpired();
        return cachedBrands;
    }

    /**
     * 缓存过期时刷新
     */
    private synchronized void refreshCacheIfExpired() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < CACHE_TTL_MS && !cachedCategories.isEmpty()) {
            return; // 缓存未过期
        }
        refreshCache();
        lastRefreshTime = now;
    }

    /**
     * 从 ES 刷新缓存（只查一次聚合，同时获取分类和品牌）
     */
    private void refreshCache() {
        try {
            SearchRequest searchRequest = new SearchRequest("items_vector");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.matchAllQuery());
            sourceBuilder.size(0);
            // 一次查询同时获取分类和品牌聚合
            sourceBuilder.aggregation(
                    AggregationBuilders.terms("all_categories").field("category").size(1000)
            );
            sourceBuilder.aggregation(
                    AggregationBuilders.terms("all_brands").field("brand").size(1000)
            );
            searchRequest.source(sourceBuilder);

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            // 解析分类
            Terms catTerms = response.getAggregations().get("all_categories");
            Set<String> categories = new HashSet<>();
            for (Terms.Bucket bucket : catTerms.getBuckets()) {
                categories.add(bucket.getKeyAsString());
            }
            this.cachedCategories = categories;

            // 解析品牌
            Terms brandTerms = response.getAggregations().get("all_brands");
            Set<String> brands = new HashSet<>();
            for (Terms.Bucket bucket : brandTerms.getBuckets()) {
                brands.add(bucket.getKeyAsString());
            }
            this.cachedBrands = brands;

            log.info("缓存刷新完成: {} 个分类, {} 个品牌", categories.size(), brands.size());
        } catch (Exception e) {
            log.warn("缓存刷新失败，使用旧缓存", e);
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建过滤条件
     */
    private BoolQueryBuilder buildFilter(String category, String brand) {
        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();

        if (StringUtils.hasText(category)) {
            filterBuilder.must(QueryBuilders.termQuery("category", category));
        }
        if (StringUtils.hasText(brand)) {
            filterBuilder.must(QueryBuilders.termQuery("brand", brand));
        }

        // 如果没有过滤条件，返回 matchAll
        if (!filterBuilder.hasClauses()) {
            filterBuilder.must(QueryBuilders.matchAllQuery());
        }

        return filterBuilder;
    }

    /**
     * 第一层：关键词快速匹配
     *
     * 从查询词中直接匹配缓存的分类和品牌名称。
     * 毫秒级响应，覆盖 90% 的常见搜索场景。
     *
     * 匹配规则：
     * - 遍历所有缓存的分类名，看查询词是否包含分类名
     * - 遍历所有缓存的品牌名，看查询词是否包含品牌名
     * - 支持中文和英文品牌名匹配
     */
    private SearchIntent matchByKeywords(String query, Set<String> allCategories, Set<String> allBrands) {
        SearchIntent intent = new SearchIntent();

        if (query == null || query.isBlank()) {
            return intent;
        }

        String q = query.toLowerCase();

        // 匹配分类：遍历所有缓存的分类名
        for (String category : allCategories) {
            if (q.contains(category.toLowerCase())) {
                intent.category = category;
                break;
            }
        }

        // 匹配品牌：遍历所有缓存的品牌名
        for (String brand : allBrands) {
            if (q.contains(brand.toLowerCase())) {
                intent.brand = brand;
                break;
            }
        }

        return intent;
    }

    /**
     * 第二层（兜底）：用 LLM 分析用户查询意图，推断 category 和 brand
     *
     * 核心思路：
     * - 不再传全量列表给 LLM（688个品牌太多了）
     * - 让 LLM 自由推断，然后用缓存验证是否存在
     * - LLM 本身就知道常见分类和品牌，不需要我们告诉它
     */
    private SearchIntent analyzeIntentWithLLM(String query, Set<String> allCategories, Set<String> allBrands) {

        SearchIntent intent = new SearchIntent();

        if (query == null || query.isBlank()) {
            return intent;
        }

        try {
            String prompt = String.format("""
                    你是一个电商搜索意图分析专家。请分析用户的搜索查询，判断他想要什么分类和品牌的商品。
                    
                    用户查询：%s
                    
                    请推断最匹配的 category（分类）和 brand（品牌）。
                    注意：category 必须是以下之一：%s
                    brand 如果不确定可以返回 null。
                    
                    只返回 JSON 格式，不要任何其他文字：
                    {"category": "推断的分类", "brand": "推断的品牌或null"}
                    """, query, String.join(", ", allCategories));

            // 调用 LLM
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .system("你是一个精确的意图分析器，只返回 JSON。")
                    .user(prompt)
                    .call()
                    .content();

            // 解析 JSON 结果
            if (response != null && !response.isBlank()) {
                String jsonStr = extractJson(response);
                if (jsonStr == null) return intent;

                @SuppressWarnings("unchecked")
                Map<String, String> result = objectMapper.readValue(jsonStr, Map.class);

                String category = result.get("category");
                String brand = result.get("brand");

                // 验证返回的值确实在缓存中
                if (category != null && !"null".equals(category) && !"无".equals(category) && allCategories.contains(category)) {
                    intent.category = category;
                }
                if (brand != null && !"null".equals(brand) && !"无".equals(brand) && allBrands.contains(brand)) {
                    intent.brand = brand;
                }
            }
        } catch (Exception e) {
            log.warn("LLM 意图分析失败，回退到无过滤搜索: {}", e.getMessage());
        }

        return intent;
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串
     */
    private String extractJson(String response) {
        if (response.contains("```json")) {
            response = response.substring(response.indexOf("```json") + 7);
            response = response.substring(0, response.indexOf("```"));
        } else if (response.contains("```")) {
            response = response.substring(response.indexOf("```") + 3);
            response = response.substring(0, response.indexOf("```"));
        }
        return response.trim();
    }

    /**
     * 搜索意图内部类
     */
    private static class SearchIntent {
        private String category;
        private String brand;

        public String getCategory() { return category; }
        public String getBrand() { return brand; }
    }




    /**
     * 解析 ES 返回结果
     */
    private List<ItemDocument> parseResults(SearchResponse response) {
        List<ItemDocument> results = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            try {
                ItemDocument item = objectMapper.convertValue(hit.getSourceAsMap(), ItemDocument.class);
                results.add(item);
            } catch (Exception e) {
                log.warn("解析 ES 结果失败: {}", e.getMessage());
            }
        }
        log.info("搜索完成，命中 {} 条结果", results.size());
        return results;
    }

    /**
     * 调用 embedding 模型生成向量
     */
    private float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }
}
