package com.hmall.interceptor;

import com.hmall.common.utils.UserContext;
import com.hmall.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求路径
        String requestURI = request.getRequestURI();
        System.out.println("=====> 拦截器收到请求: " + requestURI);

        // 2.判断是否需要登录验证（放行 Swagger/Knife4j 相关路径）
        //TODO 暂时放行
//        if (isExcludePath(requestURI)) {
//            System.out.println("=====> 放行 (Swagger): " + requestURI);
//            return true;
//        }

        // 3.获取请求头中的 token
        String token = request.getHeader("authorization");

        // 4.如果没有token，也放行（临时解决方案）
        if (token == null || token.isEmpty()) {
            System.out.println("=====> 放行 (无token): " + requestURI);
            return true;
        }

        // 5.校验token
        Long userId = jwtTool.parseToken(token);
        // 6.存入上下文
        UserContext.setUser(userId);
        // 7.放行
        return true;
    }

    private boolean isExcludePath(String requestURI) {
        // 放行 Swagger/Knife4j/springdoc 相关路径
        return requestURI.startsWith("/swagger-ui")
                || requestURI.startsWith("/swagger-ui.html")
                || requestURI.startsWith("/v3/api-docs")
                || requestURI.startsWith("/swagger-resources")
                || requestURI.startsWith("/webjars")
                || requestURI.startsWith("/doc.html")
                || requestURI.startsWith("/knife4j")
                || requestURI.startsWith("/openapi")
                || requestURI.equals("/")
                || requestURI.equals("/error")
                || requestURI.equals("/favicon.ico");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理用户
        UserContext.removeUser();
    }
}