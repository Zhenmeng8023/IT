package com.alikeyou.itmodulelogin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;

import java.util.Optional;

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
        @RequestParam(required = false) String password,
        HttpServletRequest httpServletRequest) {
        LoginRequest loginRequest = request;

        // 如果请求体中有数据，使用请求体中的数据
        if (loginRequest != null) {
            logger.info("登录请求: {}", loginRequest.getUsername());
        }
        // 否则使用URL查询参数
        else if (username != null && password != null) {
            logger.info("登录请求（URL参数）: {}", username);
            loginRequest = new LoginRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(password);
        }
        // 没有提供任何参数
        else {
            logger.info("登录请求缺少参数");
            return new LoginResponse(false, "请求参数缺失");
        }

        Optional<UserInfo> authenticatedUser = loginService.authenticate(loginRequest);
        if (authenticatedUser.isEmpty()) {
            logger.info("登录结果: 失败");
            return new LoginResponse(false, "用户名或密码错误");
        }

        UserInfo user = authenticatedUser.get();
        HttpSession oldSession = httpServletRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(LoginConstant.USER_ID, user.getId());
        session.setAttribute(LoginConstant.USER_NAME, user.getUsername());
        session.setAttribute(LoginConstant.EMAIL, user.getEmail());
        session.setAttribute(LoginConstant.ROLE_ID, user.getRoleId());

        LoginConstant.setUsername(user.getUsername());
        LoginConstant.setPassword(user.getPasswordHash());
        LoginConstant.setEmail(user.getEmail());
        LoginConstant.setUserId(user.getId());
        LoginConstant.setRoleId(user.getRoleId());

        logger.info("登录结果: 成功, sessionId={}, userId={}", session.getId(), user.getId());
        return new LoginResponse(true, "登录成功", null, user.getRoleId());
    }
    
    //登出
    @Operation(summary = "用户登出", description = "用户退出系统")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登出结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/logout")
    public LoginResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        LoginConstant.clearUserInfo();
        logger.info("用户登出成功");
        return new LoginResponse(true, "登出成功");
    }
    
    // 会话模式下保留兼容接口，明确告知前端无需再刷新 token
    @Operation(summary = "刷新登录态（会话模式下不支持）", description = "当前系统使用 HttpOnly Cookie + 服务端 Session，无需客户端刷新 token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录态刷新结果", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/api/token/refresh")
    public LoginResponse refreshToken(
        @Parameter(description = "旧Token", required = false) 
        @RequestParam(required = false) String token) {
        logger.info("接收到刷新Token请求，当前认证模式为服务端 Session，忽略旧 token");
        return new LoginResponse(false, "当前认证模式不支持刷新 Token，请使用服务端会话");
    }
}
