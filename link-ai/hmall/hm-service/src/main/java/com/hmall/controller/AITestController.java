package com.hmall.controller;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

//@RestController
//@RequestMapping("/chat")
//@RequiredArgsConstructor
public class AITestController {


    @Autowired
    @Qualifier("dashScopeChatModel")
    private  ChatModel dashScopeChatModel;

    @Autowired
    @Qualifier("openapiChatModel")
    private  ChatModel openapiChatModel;


    @GetMapping("/Dashscope")
    public Flux<String> Dashscope(@RequestParam("question") String question) {
        return dashScopeChatModel.stream(question);
    }

    @GetMapping("/OpenAI")
    public Flux<String> OpenAI(@RequestParam("question") String question) {
        return openapiChatModel.stream(question);
    }


}
