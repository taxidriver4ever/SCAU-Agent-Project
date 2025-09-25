package org.example.agentprojectjava.service;

import org.example.agentprojectjava.pojo.IdWithUUID;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface LoginService {
    public IdWithUUID LoginByPassword(String userName, String userPassword) throws InterruptedException;
    public boolean CheckIdWithUUID(String id, String uuid);
    public IdWithUUID LoginByEmail(String userEmail, String verificationCode) throws InterruptedException;
    public boolean GetVerificationCode(String userEmail);
    public boolean Logout(String id) throws InterruptedException;
}
