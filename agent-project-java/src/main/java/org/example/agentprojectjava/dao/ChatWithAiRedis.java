package org.example.agentprojectjava.dao;

import org.example.agentprojectjava.pojo.ChatContent;
import org.example.agentprojectjava.pojo.HistoryMessage;

import java.util.concurrent.CopyOnWriteArrayList;

public interface ChatWithAiRedis {
    public boolean SaveHistoryMessage(HistoryMessage message) throws InterruptedException;
    public CopyOnWriteArrayList<ChatContent> GetHistoryMessage(String uuid) throws InterruptedException;
    public boolean AddSession(HistoryMessage message) throws InterruptedException;
    public CopyOnWriteArrayList<String> GetSessions(String userId) throws InterruptedException;
    public String GetUUid(String userId,String index) throws InterruptedException;
    public boolean RemoveSession(String sessionId, String userId, String index) throws InterruptedException;
    public boolean existsIdAndUuid(String id, String uuid);
    public boolean existsSession(String sessionId);
}
