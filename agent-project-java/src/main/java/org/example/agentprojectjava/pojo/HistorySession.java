package org.example.agentprojectjava.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistorySession implements Serializable {
    private Long id;
    private Long userId;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String avatar;
}
