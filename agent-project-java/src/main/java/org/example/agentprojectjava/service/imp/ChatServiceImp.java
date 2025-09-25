package org.example.agentprojectjava.service.imp;

import jakarta.annotation.Resource;
import jodd.util.collection.MapEntry;
import org.example.agentprojectjava.dao.ChatRedis;
import org.example.agentprojectjava.mapper.ChatMapper;
import org.example.agentprojectjava.mapper.ChatWithAiMapper;
import org.example.agentprojectjava.pojo.*;
import org.example.agentprojectjava.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyStore;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChatServiceImp implements ChatService {

    @Resource
    private ChatMapper chatMapper;

    @Resource
    private ChatRedis chatRedis;

    @Override
    public CopyOnWriteArrayList<HistorySession> FindSessions(Long userId) {
        return chatMapper.SelectHistoryByUserId(userId);
    }

    @Override
    public UserInformation FindUserInformation(String userId,String email) throws InterruptedException {
        UserInformation userInformation = new UserInformation();
        userInformation.setStatus("0");
        NormalUser normalUser = chatMapper.SelectUserNameByEmail(email);

        if(normalUser != null) {
            if(chatRedis.SelectFriendRequest(normalUser.getId().toString(), userId))
                userInformation.setStatus("1");
            if (normalUser.getId() != null &&
                    (normalUser.getId().toString().equals(userId) ||
                    chatMapper.SelectIdBySenderAndReceiver(userId,normalUser.getId().toString()) != null))
                    userInformation.setStatus("1");

            userInformation.setUserName(normalUser.getUserName());
            userInformation.setAvatar(normalUser.getAvatar());
        }
        return userInformation;
    }

    @Override
    public boolean SendFriendRequest(String userId, String email) throws InterruptedException {
        return chatRedis.AddFriendRequest(userId, chatMapper.SelectUserNameByEmail(email).getId().toString());
    }

    @Override
    public CopyOnWriteArrayList<UserInformation> FindUserInformationById(String userId) throws InterruptedException {
        List<Map.Entry<String,Double>> stringDoubleConcurrentHashMap = chatRedis.GetFriendRequestByUserId(userId);
        CopyOnWriteArrayList<UserInformation> userInformations = new CopyOnWriteArrayList<>();

        long nowHour = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() / (1000 * 60 * 60);

        if(stringDoubleConcurrentHashMap != null) {
            for (Entry<String, Double> entry : stringDoubleConcurrentHashMap) {
                UserInformation userInformation = new UserInformation();
                long pastHour = entry.getValue().longValue() / (1000 * 60 * 60);

                String information = chatRedis.getUserInformation(entry.getKey().split(":")[1]);
                String[] split = information.split(":");
                String avatarString = split[1];
                String userName = information.substring(split[0].length() + split[1].length() + 2);
                userInformation.setUserName(userName);
                userInformation.setAvatar(avatarString);

                switch (entry.getKey().split(":")[0]) {
                    case "0" -> {
                        if (nowHour - pastHour >= 24 * 3) {
                            chatRedis.setUserOutOfDateRequest
                                    (userId, "2:" + entry.getKey().split(":")[1]);
                            userInformation.setStatus("2");
                        }
                        else userInformation.setStatus("0");
                    }
                    case "1" -> userInformation.setStatus("1");
                    case "2" -> userInformation.setStatus("2");
                    default -> userInformation.setStatus("0");
                }

                userInformations.add(userInformation);
            }
        }
        return userInformations;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean AcceptRequest(String userId, Long index) throws InterruptedException {
        HistorySession historySession = chatRedis.GetHistorySession(userId, index);
        if(historySession != null && historySession.getReceiverId() != null) {
            System.out.println("history session accepted");
            chatMapper.InsertHistorySession(historySession);
            HistorySession historySessionReceiver = chatRedis.GetReceiverHistorySession(userId);
            if(historySessionReceiver != null && historySessionReceiver.getReceiverId() != null) {
                historySessionReceiver.setUserId(historySession.getReceiverId());
                chatMapper.InsertHistorySession(historySessionReceiver);
            }
        }
        return chatRedis.setUserReceiveRequest(userId, index);
    }

    @Override
    public String GetSenderName(String sendId) {
        return chatRedis.GetSenderName(sendId);
    }

    @Override
    public NormalUser GetUserInformation(String id) {
        String userInformation = chatRedis.getUserInformation(id);
        if(userInformation != null) {
            NormalUser normalUser = new NormalUser();
            
            String stringEmail = userInformation.split(":")[0];
            String stringAvatar = userInformation.split(":")[1];
            String stringName = userInformation.substring(stringEmail.length() + stringAvatar.length() + 2);

            normalUser.setUserName(stringName);
            normalUser.setAvatar(stringAvatar);
            normalUser.setUserEmail(stringEmail);
            return normalUser;
        }
        return null;
    }

    @Override
    public boolean AddUserChatHistory(String senderId, String receiverId, String message) throws InterruptedException {
        return chatRedis.storeUserChatHistory(senderId,receiverId,message);
    }

    @Override
    public CopyOnWriteArrayList<ChatMessage> GetUserChatHistory(String senderId, String receiverId) throws InterruptedException {
        Set<String> strings = chatRedis.GetUserChatHistory(senderId, receiverId);
        System.out.println(strings);
        if(strings != null) {
            CopyOnWriteArrayList<ChatMessage> chatMessages = new CopyOnWriteArrayList<>();
            for (String string : strings) {
                ChatMessage chatMessage = new ChatMessage();
                String stringReceiverId = string.split(":")[0];
                String stringTime = string.split(":")[1];
                long createTime = Long.parseLong(stringTime);
                LocalDateTime localDateTime = Instant.ofEpochMilli(createTime).atZone(ZoneId.systemDefault()).toLocalDateTime();

                chatMessage.setSenderId(stringReceiverId);
                chatMessage.setContent(string.substring(stringReceiverId.length() + stringTime.length()  + 2));
                chatMessage.setCreateTime(localDateTime.toString());

                chatMessages.add(chatMessage);
            }
            System.out.println(chatMessages);
            return chatMessages;
        }
        return null;
    }
}
