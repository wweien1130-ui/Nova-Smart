package com.hmall.interceptor;

import com.hmall.common.utils.UserContext;
import com.hmall.common.utils.JwtTool;
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

        // 3.获取请求头中的 token 和 userId
        String token = request.getHeader("authorization");
        String userIdHeader = request.getHeader("userId");

        // 4.优先从 authorization token 解析用户信息
        if (token != null && !token.isEmpty()) {
            // 5.校验token
            Long userId = jwtTool.parseToken(token);
            // 6.存入上下文
            UserContext.setUser(userId);
            System.out.println("=====> 从token解析用户: " + userId);
        } else if (userIdHeader != null && !userIdHeader.isEmpty()) {
            // Feign 内部调用，直接从 header 获取 userId
            Long userId = Long.parseLong(userIdHeader);
            UserContext.setUser(userId);
            System.out.println("=====> 从header获取用户: " + userId);
        } else {
            // 3.尝试从请求参数获取 userId（兼容前端直接传 userId 的情况）
            String userIdParam = request.getParameter("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdParam);
                    UserContext.setUser(userId);
                    System.out.println("=====> 从请求参数获取用户: " + userId);
                } catch (NumberFormatException e) {
                    System.out.println("=====> 拒绝访问 (userId参数格式错误): " + requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\":\"未授权：无有效的认证信息\"}");
                    return false;
                }
            } else {
                // 4.如果没有token且没有userId header，拒绝访问
                System.out.println("=====> 拒绝访问 (无token且无userId): " + requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\":\"未授权：需要登录或提供userId\"}");
                return false;
            }
        }
        // 5.放行
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