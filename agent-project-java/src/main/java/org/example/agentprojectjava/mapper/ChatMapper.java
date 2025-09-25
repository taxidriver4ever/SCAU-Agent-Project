package org.example.agentprojectjava.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.agentprojectjava.pojo.HistorySession;
import org.example.agentprojectjava.pojo.NormalUser;

import java.util.concurrent.CopyOnWriteArrayList;

@Mapper
public interface ChatMapper {
    public CopyOnWriteArrayList<HistorySession> SelectHistoryByUserId(Long userId);
    public NormalUser SelectUserNameByEmail(String userEmail);
    public Long SelectIdBySenderAndReceiver(String userId, String receiverId);
    public void InsertHistorySession(HistorySession hs);
    public NormalUser SelectNameAndAvatarById(Long id);
}
