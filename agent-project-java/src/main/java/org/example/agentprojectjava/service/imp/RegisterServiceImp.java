package org.example.agentprojectjava.service.imp;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.LoginAndRegisterRedis;
import org.example.agentprojectjava.mapper.RegisterMapper;
import org.example.agentprojectjava.pojo.NormalUser;
import org.example.agentprojectjava.service.RegisterService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class RegisterServiceImp implements RegisterService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RegisterMapper registerMapper;

    @Resource
    private RedissonClient redisson;

    @Resource
    private LoginAndRegisterRedis loginAndRegisterRedis;

    @Override
    public boolean GetVerificationCodeForRegister(String userEmail) {
        String idWithEmail = "0:" + userEmail;
        String exchange = "direct_email_exchange";
        rabbitTemplate.convertAndSend(exchange,"verificationCode",idWithEmail);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean register(NormalUser user) throws InterruptedException {
        RLock lock = redisson.getLock("register");
        boolean b = lock.tryLock(5, 30, TimeUnit.SECONDS);
        if(b) {
            try {
                Long i = registerMapper.checkUser(user.getUserName(), user.getUserEmail());
                if(i == null && loginAndRegisterRedis.existsVerificationCodeForRegister(user.getUserEmail(), user.getCode())) {
                    registerMapper.register(user.getUserName(), user.getUserPassword(), user.getUserEmail());
                    return true;
                }
                return false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
