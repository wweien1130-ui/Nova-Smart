package com.hmall.api.client;

import com.hmall.common.domain.dto.LoginFormDTO;
import com.hmall.common.domain.vo.UserLoginVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "hmall-service", path = "/users", configuration = com.hmall.api.config.DefaultFeignConfig.class)
public interface UserFeignClient {

    @PostMapping("/login")
    UserLoginVO login(@RequestBody LoginFormDTO loginFormDTO);
}
