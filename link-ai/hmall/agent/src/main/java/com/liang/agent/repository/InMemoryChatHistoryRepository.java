package com.liang.agent.repository;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    private final Map<String,List<String>> chatHistory = new HashMap<>();
    @Override
    public void save(String type, String chatId) {
       /* if(!chatHistory.containsKey(type)){
            chatHistory.put(type,new ArrayList<>());
        }
        List<String> chatIds = chatHistory.get(type);*/
        List<String> chatIds = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        if(chatIds.contains(chatId)){
               return;
        }
        chatIds.add(chatId);

    }

    @Override
    public List<String> getChatId(String type) {
        /*List<String> strings = chatHistory.get(type);
        return strings == null ? new ArrayList<>() : strings;*/
        return chatHistory.getOrDefault(type,new ArrayList<>());
    }
}
