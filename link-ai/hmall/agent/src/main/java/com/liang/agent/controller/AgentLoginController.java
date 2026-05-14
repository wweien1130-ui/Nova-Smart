package com.liang.agent.controller;

import com.hmall.api.client.UserFeignClient;
import com.hmall.common.domain.dto.LoginFormDTO;
import com.hmall.common.domain.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AgentLoginController {

    private final UserFeignClient userFeignClient;

    @PostMapping("/login")
    public UserLoginVO login(@RequestBody LoginFormDTO loginFormDTO) {
        return userFeignClient.login(loginFormDTO);
    }
}