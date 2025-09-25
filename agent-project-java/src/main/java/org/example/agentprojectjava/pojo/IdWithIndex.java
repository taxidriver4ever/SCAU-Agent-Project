package org.example.agentprojectjava.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdWithIndex {
    private Long id;
    private Long index;
}
