package com.liang.agent.tool;

import com.hmall.api.client.ItemFeignClient;
import com.hmall.common.domain.dto.ItemDTO;
import com.liang.agent.config.ToolResultHolder;
import com.liang.agent.constants.Constant;
import com.liang.agent.converters.ItemConverter;
import com.liang.agent.result.ItemInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemSearchTool {

    private final ItemFeignClient itemFeignClient;
    private final RestTemplate restTemplate;

    @Value("${tj.es.base-url:http://localhost:8088}")
    private String esBaseUrl;

    @Tool(name = "searchItems", description = "根据关键词搜索商品，返回商品列表")
    public List<ItemInfo> searchItems(@ToolParam(description = "搜索关键词") String keyword) {
        log.info("========== 调用了 searchItems，关键词: {} ==========", keyword);

        // 1. 先尝试 ES 分词快速查询
        try {
            String url = esBaseUrl + "/rag/search?q={query}&topK={topK}";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, keyword, 10);

            if (response != null && response.get("items") != null) {
                List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) response.get("items");
                if (!itemMaps.isEmpty()) {
                    List<ItemDTO> results = itemMaps.stream()
                            .map(this::mapToItemDTO)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    log.info("ES 分词查询完成，命中 {} 条结果", results.size());
                    return ItemConverter.toItemInfoList(results);
                }
            }
        } catch (Exception e) {
            log.warn("ES 分词查询失败 ({}), 回退到 MySQL 搜索", e.getMessage());
        }

        // 2. 兜底：Feign 调用 MySQL 模糊查询
        return ItemConverter.toItemInfoList(itemFeignClient.searchByKeyword(keyword));
    }

    private ItemDTO mapToItemDTO(Map<String, Object> map) {
        try {
            ItemDTO dto = new ItemDTO();
            dto.setId(map.get("id") != null ? Long.valueOf(map.get("id").toString()) : null);
            dto.setName((String) map.get("name"));
            String priceStr = (String) map.get("price");
            if (priceStr != null) {
                try {
                    dto.setPrice((int) (Double.parseDouble(priceStr) * 100));
                } catch (NumberFormatException e) {
                    dto.setPrice(0);
                }
            }
            dto.setCategory((String) map.get("category"));
            dto.setBrand((String) map.get("brand"));
            dto.setSpec((String) map.get("spec"));
            dto.setImage((String) map.get("image"));
            return dto;
        } catch (Exception e) {
            log.warn("转换 ES 结果失败: {}", e.getMessage());
            return null;
        }
    }

    @Tool(name = Constant.Tools.QUERY_ITEM_BY_ID, description = "根据商品ID查询商品详情")
    public ItemInfo getItemDetail(
            @ToolParam(description = "商品ID") Long itemId,
            ToolContext toolContext) {

        return Optional.ofNullable(itemId)
                .map(itemFeignClient::queryItemById)
                .map(ItemConverter::toItemInfo)
                .map(itemInfo -> {
                    String field = String.format("%s_%d", ItemInfo.class.getSimpleName(), itemInfo.getId());
                    String requestId = String.valueOf(toolContext.getContext().get("requestId"));
                    ToolResultHolder.put(requestId, field, itemInfo);
                    return itemInfo;
                })
                .orElse(null);
    }
}