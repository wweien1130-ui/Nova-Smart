package com.liang.agent.repository;


import java.util.List;


public interface ChatHistoryRepository {

    /**
     * 保存聊天记录
     * @param type 聊天类型
     * @param chatId 聊天ID
     */
    void save(String type,String chatId);

    /**
     * 获取回话ID
     * @param type 聊天ID
     * @return 聊天记录列表
     */
    List<String> getChatId(String type);
}
