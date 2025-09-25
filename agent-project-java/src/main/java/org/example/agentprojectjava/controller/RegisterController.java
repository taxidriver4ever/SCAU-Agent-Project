package org.example.agentprojectjava.controller;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.pojo.EmailWithCode;
import org.example.agentprojectjava.pojo.NormalResult;
import org.example.agentprojectjava.pojo.NormalUser;
import org.example.agentprojectjava.service.RegisterService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/register")
public class RegisterController {

    @Resource
    private RegisterService registerService;

    @PostMapping("/GetVerificationCodeForRegister")
    public NormalResult GetVerificationCodeForRegister(@RequestBody EmailWithCode email) {
        if(registerService.GetVerificationCodeForRegister(email.getUserEmail())) return NormalResult.success();
        return NormalResult.error("");
    }

    @PostMapping("/Register")
    public NormalResult Register(@RequestBody NormalUser user) throws InterruptedException {
        boolean register = registerService.register(user);
        if(register) return NormalResult.success();
        return NormalResult.error("");
    }
}
