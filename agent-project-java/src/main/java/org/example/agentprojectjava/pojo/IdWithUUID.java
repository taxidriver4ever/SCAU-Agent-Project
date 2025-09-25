package org.example.agentprojectjava.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdWithUUID implements Serializable {
    private String id;
    private String uuid;
}
