package com.alikeyou.itmodulelogin.service;

import org.springframework.stereotype.Service;

import com.alikeyou.itmodulelogin.utils.JwtUtil;

@Service
public class TokenService {

    public String generateToken(String username) {
        return JwtUtil.generateToken(username);
    }

    public boolean validateToken(String token) {
        try {
            return !JwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return JwtUtil.getUsernameFromToken(token);
    }
}