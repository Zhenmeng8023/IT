package com.alikeyou.itmodulelogin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {


    //登录
    @PostMapping("/login")
    public String login() {
        return "success";
    }
}
