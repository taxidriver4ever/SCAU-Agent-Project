package org.example.agentprojectjava.service;

import org.example.agentprojectjava.pojo.*;

import java.util.concurrent.CopyOnWriteArrayList;

public interface ChatService {
    public CopyOnWriteArrayList<HistorySession> FindSessions(Long userId);
    public UserInformation FindUserInformation(String userId,String email) throws InterruptedException;
    public boolean SendFriendRequest(String userId,String email) throws InterruptedException;
    public CopyOnWriteArrayList<UserInformation> FindUserInformationById(String userId) throws InterruptedException;
    public boolean AcceptRequest(String userId,Long index) throws InterruptedException;
    public String GetSenderName(String sendId);
    public NormalUser GetUserInformation(String id);
    public boolean AddUserChatHistory(String senderId,String receiverId,String message) throws InterruptedException;
    public CopyOnWriteArrayList<ChatMessage> GetUserChatHistory(String senderId,String receiverId) throws InterruptedException;
}
