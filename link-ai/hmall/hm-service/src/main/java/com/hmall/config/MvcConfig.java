package com.hmall.config;

import cn.hutool.core.collection.CollUtil;
import com.hmall.interceptor.LoginInterceptor;
import com.hmall.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class MvcConfig implements WebMvcConfigurer {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;

/*    @Bean
    public CommonExceptionAdvice commonExceptionAdvice(){
        return new CommonExceptionAdvice();
    }*/

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1.添加拦截器
        LoginInterceptor loginInterceptor = new LoginInterceptor(jwtTool);
        InterceptorRegistration registration = registry.addInterceptor(loginInterceptor);
        // 2.配置拦截路径
        List<String> includePaths = authProperties.getIncludePaths();
        if (CollUtil.isNotEmpty(includePaths)) {
            //TODO:零食禁用登录拦截器
//            registration.addPathPatterns(includePaths);
        }
        // 3.配置放行路径
        List<String> excludePaths = authProperties.getExcludePaths();
        if (CollUtil.isNotEmpty(excludePaths)) {
            registration.excludePathPatterns(excludePaths);
        } else {
            // 如果没有配置文件中的排除路径，使用默认的排除路径
            registration.excludePathPatterns(
                    "/",
                    "/error",
                    "/favicon.ico",
                    "/v2/**",
                    "/v3/**",
                    "/swagger-resources/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/doc.html",
                    "/knife4j/**",
                    "/v2/api-docs/**",
                    "/v3/api-docs/**",
                    "/swagger-resources",
                    "/csrf"
            );
        }

    }
}