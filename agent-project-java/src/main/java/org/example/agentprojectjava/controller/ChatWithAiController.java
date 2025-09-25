package org.example.agentprojectjava.controller;


import jakarta.annotation.Resource;
import org.example.agentprojectjava.pojo.ChatContent;
import org.example.agentprojectjava.pojo.HistoryMessage;
import org.example.agentprojectjava.pojo.NormalResult;
import org.example.agentprojectjava.service.ChatWithAiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@CrossOrigin
@RestController
@RequestMapping("/ai")
public class ChatWithAiController {

    @Resource
    private ChatWithAiService chatWithAiService;

    @GetMapping("/nothing")
    public void Nothing(){}

    @PostMapping("/SaveMessage")
    public NormalResult SaveMessage(@RequestBody HistoryMessage message) throws InterruptedException {
        String uuid = chatWithAiService.SaveHistoryMessage(message);
        if (uuid != null) {
            if(Objects.equals(uuid, "Deleted")) return NormalResult.error("The session has been deleted.");
            return NormalResult.success(uuid);
        }
        return NormalResult.error("Save message failed");
    }

    @PostMapping("/AddNewSession")
    public NormalResult AddNewSession(@RequestBody HistoryMessage message) throws InterruptedException {
        boolean b = chatWithAiService.AddNewSession(message);
        if (b) return NormalResult.success(message.getHistoryMessage());
        else return NormalResult.error("Add new session failed");
    }

    @GetMapping("/ShowHistory")
    public NormalResult ShowHistory(@RequestParam String id) throws InterruptedException {
        CopyOnWriteArrayList<ChatContent> chatContents = chatWithAiService.GetHistoryMessages(id);
        if(chatContents != null) return NormalResult.success(chatContents);
        else return NormalResult.error("Get history failed");
    }

    @GetMapping("/GetConversationHistory")
    public NormalResult GetConversationHistory(@RequestParam String userId) throws InterruptedException {
        CopyOnWriteArrayList<String> strings = chatWithAiService.GetSessions(userId);
        if(strings != null) return NormalResult.success(strings);
        else return NormalResult.error("Get history failed");
    }

    @GetMapping("/GetUUid")
    public NormalResult GetUUid(@RequestParam String userId, @RequestParam String index) throws InterruptedException {
        String string = chatWithAiService.GetUUid(userId, index);
        if(string != null) return NormalResult.success(string);
        return NormalResult.error("Get uid failed");
    }

    @DeleteMapping("/DeleteHistory")
    public NormalResult DeleteHistory(@RequestParam String sessionId,
                                      @RequestParam String userId,
                                      @RequestParam String index) throws InterruptedException {
        boolean b = chatWithAiService.RemoveSession(sessionId, userId, index);
        if(b) return NormalResult.success();
        return NormalResult.error("Delete history failed");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAIResponse(
            @RequestParam String message,
            @RequestParam String sessionId,
            @RequestParam String userId,
            @RequestParam String id,
            @RequestParam String uuid) throws NoSuchAlgorithmException {

        // 验证用户身份


        if (!chatWithAiService.validateUser(id, uuid))
            return Flux.error(new RuntimeException("Unauthorized"));
        // 模拟 AI 流式响应
        return chatWithAiService.streamAIResponse(message,sessionId);
    }
}
