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
    @Operation(summary = "用户登录", description = "用户登录系统")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/login")
    public LoginResponse login(
        @Parameter(description = "登录请求", required = false) 
        @RequestBody(required = false) LoginRequest request, 
        @Parameter(description = "用户名", required = false) 
        @RequestParam(required = false) String username, 
        @Parameter(description = "密码", required = false) 
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
    @Operation(summary = "用户登出", description = "用户退出系统")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登出结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/logout")
    public LoginResponse logout() {
        // 清空LoginConstant中的用户信息
        com.alikeyou.itmodulecommon.constant.LoginConstant.clearUserInfo();
        logger.info("用户登出成功");
        return new LoginResponse(true, "登出成功");
    }
    
    // 刷新Token
    @Operation(summary = "刷新Token", description = "使用旧Token刷新获取新Token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token刷新结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/api/token/refresh")
    public LoginResponse refreshToken(
        @Parameter(description = "旧Token", required = true) 
        @RequestParam String token) {
        logger.info("接收到刷新Token请求");
        
        // 验证Token
        if (token == null || token.isEmpty()) {
            logger.warn("Token为空");
            return new LoginResponse(false, "Token不能为空");
        }
        
        try {
            // 从Token中获取用户名
            String username = com.alikeyou.itmodulelogin.utils.JwtUtil.getUsernameFromToken(token);
            if (username == null) {
                logger.warn("Token无效");
                return new LoginResponse(false, "Token无效");
            }
            
            // 生成新Token
            String newToken = com.alikeyou.itmodulelogin.utils.JwtUtil.generateToken(username);
            logger.info("Token刷新成功: {}", username);
            // 获取用户角色id
            Integer roleId = com.alikeyou.itmodulecommon.constant.LoginConstant.getRoleId();
            return new LoginResponse(true, "Token刷新成功", newToken, roleId);
        } catch (Exception e) {
            logger.error("Token刷新失败: {}", e.getMessage());
            return new LoginResponse(false, "Token刷新失败");
        }
    }
}