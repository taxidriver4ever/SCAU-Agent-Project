package org.example.agentprojectjava.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.agentprojectjava.pojo.NormalUser;

@Mapper
public interface LoginMapper {
    public NormalUser LoginByPassword(String userName, String userPassword);
    public void LastLogin(Long id);
    public NormalUser LoginByEmail(String userEmail);
}
