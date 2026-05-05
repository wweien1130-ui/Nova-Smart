package com.hmall.api.client;

import com.hmall.common.domain.dto.AddressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址服务Feign客户端
 */
@FeignClient(name = "hmall-service", path = "/addresses")
public interface AddressFeignClient {

    @GetMapping
    List<AddressDTO> findMyAddresses();

    @GetMapping("/{id}")
    AddressDTO findAddressById(@PathVariable("id") Long id);

    @PostMapping
    AddressDTO addAddress(@RequestBody AddressDTO addressDTO);

    @PutMapping
    void updateAddress(@RequestBody AddressDTO addressDTO);

    @DeleteMapping("/{id}")
    void deleteAddress(@PathVariable("id") Long id);
}
