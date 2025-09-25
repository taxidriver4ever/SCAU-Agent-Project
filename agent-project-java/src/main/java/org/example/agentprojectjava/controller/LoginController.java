package org.example.agentprojectjava.controller;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.pojo.EmailWithCode;
import org.example.agentprojectjava.pojo.IdWithUUID;
import org.example.agentprojectjava.pojo.NormalResult;
import org.example.agentprojectjava.pojo.NormalUser;
import org.example.agentprojectjava.service.LoginService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/login")
public class LoginController {

    @Resource
    private LoginService loginService;

    @PostMapping("/LoginByPassword")
    public NormalResult LoginByPassword(@RequestBody NormalUser user) throws InterruptedException {
        IdWithUUID idWithUUID = loginService.LoginByPassword(user.getUserName(), user.getUserPassword());
        if(idWithUUID != null) return NormalResult.success(idWithUUID);
        else return NormalResult.error("");
    }

    @PostMapping("/CheckIdWithUUID")
    public NormalResult CheckIdWithUUID(@RequestBody IdWithUUID idWithUUID) {
        if(loginService.CheckIdWithUUID(idWithUUID.getId(),idWithUUID.getUuid())) return NormalResult.success();
        else return NormalResult.error("");
    }

    @PostMapping("/GetVerificationCode")
    public NormalResult GetVerificationCode(@RequestBody NormalUser user) {
        if(loginService.GetVerificationCode(user.getUserEmail())) return NormalResult.success();
        return NormalResult.error("");
    }

    @PostMapping("/LoginByEmail")
    public NormalResult LoginByEmail(@RequestBody EmailWithCode emailWithCode) throws InterruptedException {
        IdWithUUID idWithUUID = loginService.LoginByEmail(emailWithCode.getUserEmail(), emailWithCode.getCode());
        if(idWithUUID != null) return NormalResult.success(idWithUUID);
        else return NormalResult.error("");
    }

    @PostMapping("/Logout")
    public NormalResult Logout(@RequestBody IdWithUUID idWithUUID) throws InterruptedException {
        if(loginService.Logout(idWithUUID.getId())) return NormalResult.success();
        return NormalResult.error("");
    }
}
