package org.example.agentprojectjava.controller;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.pojo.*;
import org.example.agentprojectjava.service.ChatService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@CrossOrigin
@RestController
public class ChatController {

    @Resource
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.private")
    public void privateMessage(@Payload ChatMessage message,
                                          Principal principal) throws InterruptedException {
        System.out.println(message);
        if(principal == null || message.getReceiverId() == null || message.getReceiverName() == null) return;
        String senderId = message.getSenderId();
        String string = chatService.GetSenderName(senderId);
        message.setUserName(string);
        System.out.println(message);
        boolean b = chatService.AddUserChatHistory(senderId, message.getReceiverId(), message.getContent());
        if(!b) return;
        messagingTemplate.convertAndSendToUser(principal.getName(),"/queue/chat.private", message, createMessageHeaders());
        messagingTemplate.convertAndSendToUser(message.getReceiverId(),"/queue/chat.private", message, createMessageHeaders());
    }

    // 创建包含跟踪信息的消息头
    private MessageHeaders createMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("message-id", UUID.randomUUID().toString());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("tracking", true);
        return new MessageHeaders(headers);
    }

    @PostMapping("/chat/FindAllSessions")
    private NormalResult FindAllSession(@RequestBody HistorySession session) {
        CopyOnWriteArrayList<HistorySession> historySessions = chatService.FindSessions(session.getUserId());
        return NormalResult.success(historySessions);
    }

    @PostMapping("/chat/SelectUser")
    private NormalResult SelectUser(@RequestBody NormalUser user) throws InterruptedException {
        UserInformation userInformation = chatService.FindUserInformation(user.getId().toString(), user.getUserEmail());
        if(userInformation.getUserName() == null) return NormalResult.error("User not found");
        return NormalResult.success(userInformation);
    }

    @PostMapping("/chat/SendFriendRequest")
    private NormalResult SendFriendRequest(@RequestBody NormalUser user) throws InterruptedException {
        boolean b = chatService.SendFriendRequest(user.getId().toString(), user.getUserEmail());
        if(b) return NormalResult.success();
        return NormalResult.error("Send friend request failed");
    }

    @PostMapping("/chat/GetNewFriendsInformation")
    private NormalResult GetNewFriends(@RequestBody NormalUser user) throws InterruptedException {
        CopyOnWriteArrayList<UserInformation> userInformations = chatService.FindUserInformationById(user.getId().toString());
        return NormalResult.success(userInformations);
    }

    @PostMapping("/chat/AcceptRequest")
    private NormalResult AcceptRequest(@RequestBody IdWithIndex idWithIndex) throws InterruptedException {
        if(chatService.AcceptRequest(idWithIndex.getId().toString(),idWithIndex.getIndex())) return NormalResult.success();
        return NormalResult.error("Accept request failed");
    }

    @PostMapping("/chat/GetUserInformation")
    private NormalResult GetUserInformation(@RequestBody NormalUser user) throws InterruptedException {
        NormalUser normalUser = chatService.GetUserInformation(user.getId().toString());
        if(normalUser == null) return NormalResult.error("User not found");
        else return NormalResult.success(normalUser);
    }

    @PostMapping("/chat/GetChatHistory")
    private NormalResult GetChatHistory(@RequestBody ChatMessage chatMessage) throws InterruptedException {
        CopyOnWriteArrayList<ChatMessage> chatMessages = chatService.GetUserChatHistory(
                chatMessage.getSenderId(),
                chatMessage.getReceiverId()
        );
        return NormalResult.success(chatMessages);
    }
}
