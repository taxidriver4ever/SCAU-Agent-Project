package org.example.agentprojectjava.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceMessage implements java.io.Serializable {
    private String content;
    private String userId;
    private String timestamp;
    private String status;
    private String avatar;
    private String image;
}
