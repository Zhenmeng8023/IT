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
import com.alikeyou.itmodulelogin.dto.VerifyCodeRequest;
import com.alikeyou.itmodulelogin.service.LoginService;
import com.alikeyou.itmodulelogin.service.VerifyCodeService;

@RestController
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private LoginService loginService;

    @Autowired
    private VerifyCodeService verifyCodeService;

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

    //发送验证码
    @PostMapping("/send-verify-code")
    public LoginResponse sendVerifyCode(@RequestBody VerifyCodeRequest request) {
        String email = request.getEmail();
        logger.info("接收到发送验证码请求，邮箱：{}", email);
        
        // 验证邮箱格式
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确：{}", email);
            return new LoginResponse(false, "邮箱格式不正确");
        }
        
        logger.info("邮箱格式验证通过，开始处理发送验证码请求");
        boolean result = verifyCodeService.sendVerifyCode(email);
        
        if (result) {
            logger.info("验证码发送成功: {}", email);
            return new LoginResponse(true, "验证码发送成功");
        } else {
            logger.info("验证码发送失败: {}", email);
            return new LoginResponse(false, "验证码发送失败");
        }
    }

    //注册
    @PostMapping("/register")
    public LoginResponse register(@RequestParam String username, 
                                 @RequestParam String password, 
                                 @RequestParam String email, 
                                 @RequestParam String verifyCode) {
        logger.info("注册请求: {}, {}", username, email);
        
        // 验证验证码
        boolean codeValid = verifyCodeService.verifyCode(email, verifyCode);
        if (!codeValid) {
            logger.info("验证码验证失败: {}", email);
            return new LoginResponse(false, "验证码错误");
        }
        
        // 调用LoginService的register方法进行注册
        LoginResponse response = loginService.register(username, password, email);
        
        if (response.isSuccess()) {
            logger.info("注册成功: {}", username);
        } else {
            logger.info("注册失败: {}, 原因: {}", username, response.getMessage());
        }
        
        return response;
    }
}