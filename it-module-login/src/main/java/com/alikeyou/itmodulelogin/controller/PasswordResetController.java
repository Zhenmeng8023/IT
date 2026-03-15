package com.alikeyou.itmodulelogin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.dto.VerifyCodeRequest;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulelogin.service.VerifyCodeService;
import com.alikeyou.itmodulelogin.service.RegistService;

import java.util.Optional;

@RestController
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private VerifyCodeService verifyCodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistService registService;

    // 发送找回密码验证码
    @Operation(summary = "发送找回密码验证码", description = "向指定邮箱发送找回密码验证码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证码发送结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/password_reset/send-verify-code")
    public LoginResponse sendResetPasswordVerifyCode(
        @Parameter(description = "验证码请求", required = true) 
        @RequestBody VerifyCodeRequest request) {
        String email = request.getEmail();
        logger.info("接收到发送找回密码验证码请求，邮箱：{}", email);
        
        // 验证邮箱格式
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确：{}", email);
            return new LoginResponse(false, "邮箱格式不正确");
        }

        // 检查邮箱是否存在
        Optional<UserInfo> existingUser = userRepository.findByEmail(email);
        if (!existingUser.isPresent()) {
            logger.warn("邮箱未被注册：{}", email);
            return new LoginResponse(false, "邮箱未被注册");
        }

        logger.info("邮箱验证通过，开始处理发送验证码请求");
        boolean result = verifyCodeService.sendVerifyCode(email);
        
        if (result) {
            logger.info("验证码发送成功: {}", email);
            return new LoginResponse(true, "验证码发送成功");
        } else {
            logger.info("验证码发送失败: {}", email);
            return new LoginResponse(false, "验证码发送失败");
        }
    }

    // 验证找回密码令牌
    @Operation(summary = "验证找回密码令牌", description = "验证找回密码令牌是否有效")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "令牌验证结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @GetMapping("/password_reset/verify-token")
    public LoginResponse verifyResetToken(
        @Parameter(description = "邮箱", required = true) 
        @RequestParam String email,
        @Parameter(description = "验证码", required = true) 
        @RequestParam String token) {
        logger.info("接收到验证找回密码令牌请求，邮箱：{}", email);
        
        // 验证参数
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确：{}", email);
            return new LoginResponse(false, "邮箱格式不正确");
        }
        
        if (token == null || token.length() != 6) {
            logger.warn("令牌格式不正确: {}", token);
            return new LoginResponse(false, "令牌长度必须为6位");
        }
        
        // 验证验证码
        boolean codeValid = verifyCodeService.verifyCode(email, token);
        if (!codeValid) {
            logger.info("令牌验证失败: {}", email);
            return new LoginResponse(false, "令牌错误或已过期");
        }
        
        logger.info("令牌验证成功: {}", email);
        return new LoginResponse(true, "令牌验证成功");
    }

    // 提交新密码
    @Operation(summary = "提交新密码", description = "提交新密码以重置用户密码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "密码重置结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/password_reset/reset")
    public LoginResponse resetPassword(
        @Parameter(description = "邮箱", required = true) 
        @RequestParam String email,
        @Parameter(description = "验证码", required = true) 
        @RequestParam String token,
        @Parameter(description = "新密码", required = true) 
        @RequestParam String newPassword) {
        logger.info("接收到重置密码请求，邮箱：{}", email);
        
        // 验证参数
        if (email == null || !email.contains("@")) {
            logger.warn("邮箱格式不正确：{}", email);
            return new LoginResponse(false, "邮箱格式不正确");
        }
        
        if (token == null || token.length() != 6) {
            logger.warn("令牌格式不正确: {}", token);
            return new LoginResponse(false, "令牌长度必须为6位");
        }
        
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 20) {
            logger.warn("密码格式不正确");
            return new LoginResponse(false, "密码长度必须在8-20之间");
        }
        
        // 验证验证码
        boolean codeValid = verifyCodeService.verifyCode(email, token);
        if (!codeValid) {
            logger.info("令牌验证失败: {}", email);
            return new LoginResponse(false, "令牌错误或已过期");
        }
        
        // 查找用户
        Optional<UserInfo> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            logger.warn("用户不存在：{}", email);
            return new LoginResponse(false, "用户不存在");
        }
        
        // 更新密码
        UserInfo user = userOptional.get();
        boolean updateResult = registService.updatePassword(user.getId(), newPassword);
        
        if (updateResult) {
            logger.info("密码重置成功: {}", email);
            return new LoginResponse(true, "密码重置成功");
        } else {
            logger.info("密码重置失败: {}", email);
            return new LoginResponse(false, "密码重置失败");
        }
    }
}