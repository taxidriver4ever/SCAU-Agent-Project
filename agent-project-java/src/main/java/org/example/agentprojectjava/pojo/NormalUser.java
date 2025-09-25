package org.example.agentprojectjava.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NormalUser implements Serializable {
    private Long id;
    private String userName;
    private String userPassword;
    private String userEmail;
    private String createdAt;
    private String updatedAt;
    private String lastLogin;
    private int status;
    private String code;
    private String avatar;
}
