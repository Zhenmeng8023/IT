package com.alikeyou.itmodulelogin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alikeyou.itmodulelogin.dto.LoginRequest;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.service.LoginService;

@RestController
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private LoginService loginService;

    //登录
    @PostMapping("/login")
    public LoginResponse login(@RequestBody(required = false) LoginRequest request, 
                              @RequestParam(required = false) String username, 
                              @RequestParam(required = false) String password) {
        // 如果请求体中有数据，使用请求体中的数据
        if (request != null) {
            logger.info("登录请求: {}", request.getUsername());
            LoginResponse response = loginService.login(request);
            logger.info("登录结果: {}", response.isSuccess() ? "成功" : "失败");
            return response;
        }
        // 否则使用URL查询参数
        else if (username != null && password != null) {
            logger.info("登录请求（URL参数）: {}", username);
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(password);
            LoginResponse response = loginService.login(loginRequest);
            logger.info("登录结果: {}", response.isSuccess() ? "成功" : "失败");
            return response;
        }
        // 没有提供任何参数
        else {
            logger.info("登录请求缺少参数");
            return new LoginResponse(false, "请求参数缺失");
        }
    }
    
    //登出
    @PostMapping("/logout")
    public LoginResponse logout() {
        // 清空LoginConstant中的用户信息
        com.alikeyou.itmodulecommon.constant.LoginConstant.clearUserInfo();
        logger.info("用户登出成功");
        return new LoginResponse(true, "登出成功");
    }
}