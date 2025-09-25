package org.example.agentprojectjava.dao;

import org.example.agentprojectjava.pojo.NormalUser;

public interface LoginAndRegisterRedis {
    public void storeIdAndUuid(Long id, String uuid) throws InterruptedException;
    public boolean existsIdAndUuid(String id, String uuid);
    public boolean deleteIdAndUuid(String id) throws InterruptedException;
    public void storeVerificationCode(Long id, String verificationCode) throws InterruptedException;
    public boolean existsVerificationCode(Long id, String verificationCode);
    public void storeVerificationCodeForRegister(String userEmail, String verificationCode) throws InterruptedException;
    public boolean existsVerificationCodeForRegister(String userEmail, String verificationCode);
    public void storeUserInformation(NormalUser normalUser) throws InterruptedException;
}
