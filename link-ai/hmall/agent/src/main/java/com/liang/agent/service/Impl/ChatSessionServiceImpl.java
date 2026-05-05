package com.liang.agent.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.utils.UserContext;
import com.liang.agent.config.SessionProperties;
import com.liang.agent.domain.entity.ChatSession;
import com.liang.agent.domain.vo.ChatSessionVO;
import com.liang.agent.domain.vo.MessageVO;
import com.liang.agent.domain.vo.SessionVO;
import com.liang.agent.enums.MessageTypeEnum;
import com.liang.agent.mapper.ChatSessionMapper;
import com.liang.agent.memory.MyAssistantMessage;
import com.liang.agent.service.ChatService;
import com.liang.agent.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {


    private final SessionProperties sessionProperties;

    /**
     * 创建会话session
     *
     * @param n 热门问题的数量
     * @param userId 用户ID
     * @return 会话信息
     */
    @Override
    public SessionVO createSession(Integer n, Long userId) {
        SessionVO sessionVo = BeanUtil.toBean(sessionProperties, SessionVO.class);
        //随机获取examples
        sessionVo.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), n));

        //随机生成sessionId
        sessionVo.setSessionId(IdUtil.fastSimpleUUID());

        //构建持久化对象，并持久化
        // userId 为 null 时不创建会话，由调用方保证 userId 不为 null
        if (userId == null) {
            throw new RuntimeException("请先登录后再操作");
        }
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionVo.getSessionId())
                .userId(userId)
                .creater(userId)
                .updater(userId)
                .build();
        super.save(chatSession);  // ← 保存这个完整的对象

        return sessionVo;
    }

    /**
     * 获取热门会话
     *
     * @return 热门会话列表
     */
    @Override
    public List<SessionVO.Example> hotExamples(Integer n) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), n);
    }


    private final ChatMemory chatMemory;

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 根据会话ID获取对话ID
        String conversationId = ChatService.getConversationId(sessionId, UserContext.getUser());
        // 从Redis中获取历史消息
        List<Message> messageList = this.chatMemory.get(conversationId);
        // 过滤并转换消息列表
        return StreamUtil.of(messageList)
                // 过滤掉非用户消息和助手消息
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                // 转换为MessageVO对象
                /*.map(message ->
                        MessageVO.builder()
                        .content(message.getText())
                        .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .build())
                .toList();*/
                .map(message -> {
                    if (message instanceof MyAssistantMessage) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((MyAssistantMessage) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .toList();
    }


    /**
     * 异步更新聊天会话的标题
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    @Async
    @Override
    public void update(String sessionId, String title, Long userId) {
        log.info("update called - sessionId: {}, title: {}, userId: {}", sessionId, title, userId);

        // 只根据 sessionId 查询，因为 sessionId 本身是唯一的
        // 不使用 userId 作为查询条件，避免 createSession 时 userId 为 null 被设为 0 导致查询不一致
        List<ChatSession> list = super.lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .list();

        log.info("query result size: {}", list.size());

        if (CollUtil.isEmpty(list)) {
            log.warn("No chat session found for sessionId: {}", sessionId);
            return;
        }

        ChatSession chatSession = list.get(0);
        log.info("Before update - existing title: {}", chatSession.getTitle());

        if (StrUtil.isEmpty(chatSession.getTitle()) && !StrUtil.isEmpty(title)) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
            chatSession.setUpdateTime(LocalDateTimeUtil.now());
            boolean result = super.updateById(chatSession);
            log.info("Update result: {}, new title: {}", result, chatSession.getTitle());
        }
    }




    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        var userId = UserContext.getUser();
        // 查询历史会话，限制返回条数
        var list = super.lambdaQuery()
                .eq(ChatSession::getUserId, UserContext.getUser())
                .isNotNull(ChatSession::getTitle)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT 30")
                .list();

        if (CollUtil.isEmpty(list)) {
            log.info("No chat sessions found for user: {}", userId);
            return Map.of();
        }


        // 转换为 ChatSessionVO 列表
        var chatSessionVOS = CollStreamUtil.toList(list, chatSession ->
                ChatSessionVO.builder()
                        .sessionId(chatSession.getSessionId())
                        .title(chatSession.getTitle())
                        .updateTime(chatSession.getUpdateTime())
                        .build()
        );

        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";

        // 当前时间
        var now = LocalDateTime.now().toLocalDate();

        // 按照更新时间分组
        return CollStreamUtil.groupByKey(chatSessionVOS, vo -> {
            // 计算两个日期之间的天数差
            long between = Math.abs(ChronoUnit.DAYS.between(vo.getUpdateTime().toLocalDate(), now));
            if (between == 0) {
                return TODAY;
            } else if (between <= 30) {
                return LAST_30_DAYS;
            } else if (between <= 365) {
                return LAST_YEAR;
            } else {
                return MORE_THAN_YEAR;
            }
        });
    }




    @Override
    public void deleteHistorySession(String sessionId) {
        //删除数据库的数据
        var queryWrapper = Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser());
        super.remove(queryWrapper);

        //删除redis中的数据
        var conversationId = ChatService.getConversationId(sessionId, UserContext.getUser());
        this.chatMemory.clear(conversationId);
    }




    @Override
    public void updateTitle(String sessionId, String title) {
        //更新数据
        super.lambdaUpdate()
                // 设置更新条件, 更新字段为title(最多设置前100个字符)，更新条件为sessionId和userId
                .set(ChatSession::getTitle, StrUtil.sub(title, 0, 100))
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser())
                .update();
    }
}
