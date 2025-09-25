package org.example.agentprojectjava.service.imp;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.LoginAndRegisterRedis;
import org.example.agentprojectjava.mapper.LoginMapper;
import org.example.agentprojectjava.pojo.IdWithUUID;
import org.example.agentprojectjava.pojo.NormalUser;
import org.example.agentprojectjava.service.LoginService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoginServiceImp implements LoginService {

    @Resource
    private LoginMapper loginMapper;

    @Resource
    private LoginAndRegisterRedis loginAndRegisterRedis;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public IdWithUUID LoginByPassword(String userName, String userPassword) throws InterruptedException {
        NormalUser normalUser = loginMapper.LoginByPassword(userName, userPassword);
        if(normalUser != null){
            String uuid = UUID.randomUUID().toString();
            loginAndRegisterRedis.storeIdAndUuid(normalUser.getId(),uuid);
            loginAndRegisterRedis.storeUserInformation(normalUser);
            loginMapper.LastLogin(normalUser.getId());
            return new IdWithUUID(Long.toString(normalUser.getId()),uuid);
        }
        return null;
    }

    @Override
    public boolean CheckIdWithUUID(String id, String uuid) {
        return loginAndRegisterRedis.existsIdAndUuid(id, uuid);
    }

    @Override
    public IdWithUUID LoginByEmail(String userEmail, String verificationCode) throws InterruptedException {
        NormalUser normalUser = loginMapper.LoginByEmail(userEmail);
        if(normalUser != null && loginAndRegisterRedis.existsVerificationCode(normalUser.getId(),verificationCode)) {
            String uuid = UUID.randomUUID().toString();
            loginAndRegisterRedis.storeIdAndUuid(normalUser.getId(),uuid);
            loginAndRegisterRedis.storeUserInformation(normalUser);
            loginMapper.LastLogin(normalUser.getId());
            return new IdWithUUID(Long.toString(normalUser.getId()),uuid);
        }
        return null;
    }

    @Override
    public boolean GetVerificationCode(String userEmail) {
        NormalUser normalUser = loginMapper.LoginByEmail(userEmail);
        Long i = normalUser.getId();
        if(i != null) {
            String idWithEmail = i.toString() + ":" + userEmail;
            String exchange = "direct_email_exchange";
            rabbitTemplate.convertAndSend(exchange,"verificationCode",idWithEmail);
            return true;
        }
        return false;
    }

    @Override
    public boolean Logout(String id) throws InterruptedException {
        boolean b = loginAndRegisterRedis.deleteIdAndUuid(id);
        if(b){
            loginMapper.LastLogin(Long.parseLong(id));
            return true;
        }
        return false;
    }
}
