package com.hmall.api.client;

import com.hmall.common.domain.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "hmall-service", path = "/items")
public interface ItemFeignClient {

    @GetMapping
    List<ItemDTO> queryItemByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/{id}")
    ItemDTO queryItemById(@PathVariable("id") Long id);

    @PostMapping
    void saveItem(@RequestBody ItemDTO item);

    @PutMapping("/status/{id}/{status}")
    void updateItemStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status);

    @PutMapping
    void updateItem(@RequestBody ItemDTO item);

    @DeleteMapping("{id}")
    void deleteItemById(@PathVariable("id") Long id);

    @PutMapping("/stock/deduct")
    void deductStock(@RequestBody List<com.hmall.common.domain.dto.OrderDetailDTO> items);

    // ItemFeignClient.java 中新增
    @GetMapping("/search")
    List<ItemDTO> searchByKeyword(@RequestParam("keyword") String keyword);
}
