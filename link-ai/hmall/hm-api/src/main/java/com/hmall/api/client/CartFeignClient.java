package com.hmall.api.client;

import com.hmall.common.domain.dto.CartFormDTO;
import com.hmall.common.domain.vo.CartVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车服务Feign客户端
 */
@FeignClient(name = "hmall-service", path = "/carts")
public interface CartFeignClient {

    @PostMapping
    void addItem2Cart(@RequestBody CartFormDTO cartFormDTO);

    @GetMapping
    List<CartVO> queryMyCarts();

    @PutMapping
    void updateCart(@RequestBody CartVO cart);

    @DeleteMapping("{id}")
    void deleteCartItem(@PathVariable("id") Long id);

    @DeleteMapping
    void deleteCartItemByIds(@RequestParam("ids") List<Long> ids);
}