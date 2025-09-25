package org.example.agentprojectjava.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable {
    public enum MessageType {
        CHAT, JOIN, LEAVE, PRIVATE
    }

    private MessageType type;
    private String senderId;
    private String receiverName;
    private String content;
    private String createTime;
    private String avatar;
    private String userName;
    private String receiverId;

    public ChatMessage(MessageType type, String senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
        this.createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 省略其他getter和setter
}
