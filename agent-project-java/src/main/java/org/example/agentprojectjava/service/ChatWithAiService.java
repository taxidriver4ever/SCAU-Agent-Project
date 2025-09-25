package org.example.agentprojectjava.service;

import org.example.agentprojectjava.pojo.ChatContent;
import org.example.agentprojectjava.pojo.HistoryMessage;
import reactor.core.publisher.Flux;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ChatWithAiService {
    public String SaveHistoryMessage(HistoryMessage historyMessage) throws InterruptedException;
    public CopyOnWriteArrayList<ChatContent> GetHistoryMessages(String uuid) throws InterruptedException;
    public boolean AddNewSession(HistoryMessage historyMessage) throws InterruptedException;
    public CopyOnWriteArrayList<String> GetSessions(String userId) throws InterruptedException;
    public String GetUUid(String userId, String index) throws InterruptedException;
    public boolean RemoveSession(String sessionId, String userId, String index) throws InterruptedException;
    public boolean validateUser(String id, String uuid);
    public Flux<String> streamAIResponse(String message,String sessionId) throws NoSuchAlgorithmException;
}
