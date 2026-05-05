package com.liang.agent.util;

import com.hmall.common.utils.UserContext;
import com.liang.agent.constants.Constant;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Optional;

public class ToolContextUtil {

    private ToolContextUtil() {}

    /**
     * 从 ToolContext 中提取 userId
     * @param toolContext Spring AI 的 ToolContext
     * @return userId
     * @throws RuntimeException 如果未登录
     */
    public static Long getUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new RuntimeException("请先登录后再操作");
        }
        return Optional.ofNullable(toolContext.getContext().get(Constant.USER_ID))
                .filter(userIdObj -> userIdObj instanceof Number)
                .map(userIdObj -> ((Number) userIdObj).longValue())
                .filter(userId -> userId > 0)
                .orElseThrow(() -> new RuntimeException("请先登录后再操作"));
    }

    /**
     * 从 ToolContext 提取 userId 并设置到 UserContext（ThreadLocal）
     * @param toolContext Spring AI 的 ToolContext
     * @return userId
     * @throws RuntimeException 如果未登录
     */
    public static Long setUserContext(ToolContext toolContext) {
        Long userId = getUserId(toolContext);
        UserContext.setUser(userId);
        return userId;
    }

    /**
     * 从 ToolContext 提取 userId，不抛异常，返回 null
     * @param toolContext Spring AI 的 ToolContext
     * @return userId 或 null
     */
    public static Long getUserIdOrNull(ToolContext toolContext) {
        try {
            return getUserId(toolContext);
        } catch (Exception e) {
            return null;
        }
    }
}