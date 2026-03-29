package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.MembershipDTO;
import com.alikeyou.itmodulepayment.service.MembershipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // 检查用户是否为会员
    @GetMapping("/check-vip/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserIsVip(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        Boolean isVip = membershipService.checkUserIsVip(userId);
        response.put("isVip", isVip);
        response.put("userId", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户的会员信息
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserMembershipInfo(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserMembershipInfo(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "未找到用户会员信息");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 获取用户当前有效的会员信息
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Map<String, Object>> getUserActiveMembership(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        var membershipOpt = membershipService.getUserActiveMembership(userId);
        
        if (membershipOpt.isPresent()) {
            MembershipDTO dto = membershipOpt.get();
            response.put("success", true);
            response.put("data", dto);
        } else {
            response.put("success", false);
            response.put("message", "用户暂无有效会员");
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
