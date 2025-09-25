package org.example.agentprojectjava.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.agentprojectjava.pojo.HistoryMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mapper
public interface ChatWithAiMapper {
    public void SaveHistoryMessage(HistoryMessage historyMessage);
    public CopyOnWriteArrayList<HistoryMessage> getHistoryMessages(String sessionId);
    public void DeleteHistoryMessage(String sessionId);
    public CopyOnWriteArrayList<Integer> SearchHistoryId(String sessionId);
}
