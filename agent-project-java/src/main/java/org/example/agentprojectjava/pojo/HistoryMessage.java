package org.example.agentprojectjava.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HistoryMessage {
    private Long id;
    private Long userId;
    private LocalDateTime createTime;
    private String historyMessage;
    private String sessionId;
    private String status;
    private String avatar;
}
