package org.example.agentprojectjava.service;

import org.example.agentprojectjava.pojo.NormalUser;

public interface RegisterService {
    boolean GetVerificationCodeForRegister(String userEmail);
    boolean register(NormalUser user) throws InterruptedException;
}
