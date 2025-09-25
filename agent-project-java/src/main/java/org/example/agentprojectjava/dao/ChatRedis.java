package org.example.agentprojectjava.dao;

import org.example.agentprojectjava.pojo.HistorySession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ChatRedis {
    public boolean SelectFriendRequest(String receiverId,String userId) throws InterruptedException;
    public boolean AddFriendRequest(String userId, String email) throws InterruptedException;
    public List<Map.Entry<String,Double>> GetFriendRequestByUserId(String userId) throws InterruptedException;
    public String getUserInformation(String id);
    public void setUserOutOfDateRequest(String id, String receiverValue) throws InterruptedException;
    public boolean setUserReceiveRequest(String id, Long index) throws InterruptedException;
    public HistorySession GetHistorySession(String id, Long index) throws InterruptedException;
    public HistorySession GetReceiverHistorySession(String id) throws InterruptedException;
    public String GetSenderName(String id);
    public boolean storeUserChatHistory(String senderId,String receiverId,String message) throws InterruptedException;
    public Set<String> GetUserChatHistory(String senderId, String receiverId) throws InterruptedException;
}
