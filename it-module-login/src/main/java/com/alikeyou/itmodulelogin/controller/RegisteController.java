package com.alikeyou.itmodulelogin.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.dto.RegisterRequest;
import com.alikeyou.itmodulelogin.dto.VerifyCodeRequest;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulelogin.service.RegistService;
import com.alikeyou.itmodulelogin.service.VerifyCodeService;

import java.util.Optional;

@RestController
public class RegisteController {

    private static final Logger logger = LoggerFactory.getLogger(RegisteController.class);

    @Autowired
    private RegistService registService;

    @Autowired
    private VerifyCodeService verifyCodeService;

    @Autowired
    private UserRepository userRepository;

    //发送验证码
    @Operation(summary = "发送验证码", description = "向指定邮箱发送验证码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证码发送结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/register/send-verify-code")
    public LoginResponse sendVerifyCode(
        @Parameter(description = "验证码请求", required = true) 
        @RequestBody VerifyCodeRequest request) {
        String email = request.getEmail();
        logger.info("接收到发送验证码请求，邮箱：{}", email);
        
        // 验证邮箱格式
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确：{}", email);
            return new LoginResponse(false, "邮箱格式不正确");
        }

        // 检查邮箱是否已存在
        Optional<UserInfo> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            logger.warn("邮箱已被注册：{}", email);
            return new LoginResponse(false, "邮箱已被注册");
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
    @Operation(summary = "用户注册", description = "用户注册新账号")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注册结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/register")
    public LoginResponse register(
        @Parameter(description = "注册请求", required = true) 
        @RequestBody RegisterRequest request) {
        String username = request.getName();
        String password = request.getPassword();
        String email = request.getEmail();
        String verifyCode = request.getCode();
        
        logger.info("注册请求: {}, {}", username, email);
        
        // 验证参数
        if (username == null || username.isEmpty() || username.length() > 10) {
            logger.warn("用户名格式不正确: {}", username);
            return new LoginResponse(false, "用户名长度必须在1-10之间");
        }
        
        if (password == null || password.length() < 8 || password.length() > 20) {
            logger.warn("密码格式不正确");
            return new LoginResponse(false, "密码长度必须在8-20之间");
        }
        
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确: {}", email);
            return new LoginResponse(false, "请输入正确的邮箱格式");
        }
        
        if (verifyCode == null || verifyCode.length() != 6) {
            logger.warn("验证码格式不正确: {}", verifyCode);
            return new LoginResponse(false, "验证码长度必须为6位");
        }
        
        // 验证验证码
        boolean codeValid = verifyCodeService.verifyCode(email, verifyCode);
        if (!codeValid) {
            logger.info("验证码验证失败: {}", email);
            return new LoginResponse(false, "验证码错误");
        }
        
        // 调用RegistService的register方法进行注册
        LoginResponse response = registService.register(username, password, email);
        
        if (response.isSuccess()) {
            logger.info("注册成功: {}", username);
        } else {
            logger.info("注册失败: {}, 原因: {}", username, response.getMessage());
        }
        
        return response;
    }
}