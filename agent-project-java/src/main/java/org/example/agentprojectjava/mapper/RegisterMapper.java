package org.example.agentprojectjava.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegisterMapper {
    public void register(String userName,String userPassword,String userEmail);
    public Long checkUser(String userName,String userEmail);
}
