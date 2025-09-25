package org.example.agentprojectjava.dao.imp;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.LoginAndRegisterRedis;
import org.example.agentprojectjava.pojo.NormalUser;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Repository
public class LoginAndRegisterRedisImp implements LoginAndRegisterRedis {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redisson;

    private static final int EXPIRE_TIME = 60 * 60 * 24 * 7;

    @Override
    public void storeIdAndUuid(Long id, String uuid) throws InterruptedException {
        RLock lock = redisson.getLock("login:uuid");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                String key = "login:uuid:" + id.toString();
                redisTemplate.opsForValue().set(key, uuid, EXPIRE_TIME, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
    }

    @Override
    public boolean existsIdAndUuid(String id, String uuid) {
        return Objects.equals(redisTemplate.opsForValue().get("login:uuid:" + id), uuid);
    }

    @Override
    public boolean deleteIdAndUuid(String id) throws InterruptedException {
        RLock lock = redisson.getLock("login:uuid");
        boolean b = lock.tryLock(5, TimeUnit.SECONDS);
        if (b) {
            try {
                return redisTemplate.delete("login:uuid:" + id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
        return false;
    }

    @Override
    public void storeVerificationCode(Long id, String verificationCode) throws InterruptedException {
        RLock lock = redisson.getLock("login:VerificationCode");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                String key = "login:VerificationCode:" + id.toString();
                redisTemplate.opsForValue().set(key, verificationCode, 5 * 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
    }

    @Override
    public boolean existsVerificationCode(Long id, String verificationCode) {
        return Objects.equals(redisTemplate.opsForValue().get("login:VerificationCode:" + id.toString()), verificationCode);
    }

    @Override
    public void storeVerificationCodeForRegister(String userEmail, String verificationCode) throws InterruptedException {
        RLock lock = redisson.getLock("register:VerificationCode");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                String key = "register:VerificationCode:" + userEmail;
                redisTemplate.opsForValue().set(key, verificationCode, 5 * 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
    }

    @Override
    public boolean existsVerificationCodeForRegister(String userEmail, String verificationCode) {
        return Objects.equals(redisTemplate.opsForValue().get("register:VerificationCode:" + userEmail), verificationCode);
    }

    @Override
    public void storeUserInformation(NormalUser normalUser) throws InterruptedException {
        RLock lock = redisson.getLock("login:UserInformation");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                redisTemplate.opsForValue().set("login:UserInformation:" + normalUser.getId(),
                        normalUser.getUserEmail()+":"+normalUser.getAvatar()+":"+normalUser.getUserName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
    }

}
