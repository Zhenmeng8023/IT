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

    //注册
    @PostMapping("/register")
    public LoginResponse register(@RequestBody(required = false) LoginRequest request, 
                                 @RequestParam(required = false) String username, 
                                 @RequestParam(required = false) String password) {
        // 如果请求体中有数据，使用请求体中的数据
        if (request != null) {
            logger.info("注册请求: {}", request.getUsername());
            // 这里可以添加注册逻辑
            LoginResponse response = new LoginResponse(false, "注册功能暂未实现");
            logger.info("注册结果: {}", response.isSuccess() ? "成功" : "失败");
            return response;
        }
        // 否则使用URL查询参数
        else if (username != null && password != null) {
            logger.info("注册请求（URL参数）: {}", username);
            // 这里可以添加注册逻辑
            LoginResponse response = new LoginResponse(false, "注册功能暂未实现");
            logger.info("注册结果: {}", response.isSuccess() ? "成功" : "失败");
            return response;
        }
        // 没有提供任何参数
        else {
            logger.info("注册请求缺少参数");
            return new LoginResponse(false, "请求参数缺失");
        }
    }
}