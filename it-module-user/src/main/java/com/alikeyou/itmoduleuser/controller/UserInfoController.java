package com.alikeyou.itmoduleuser.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmoduleuser.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserInfoController {
    
    @Autowired
    private UserInfoService userInfoService;
    
    // 获取用户信息
    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable Long id) {
        Optional<UserInfo> userInfo = userInfoService.getUserById(id);
        return userInfo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    // 获取当前用户信息（使用LoginConstant中存储的用户信息）
    @GetMapping("/current")
    public ResponseEntity<UserInfo> getCurrentUser() {
        Optional<UserInfo> userInfo = userInfoService.getCurrentUser();
        return userInfo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    // 创建用户
    @PostMapping
    public ResponseEntity<UserInfo> createUser(@RequestBody UserInfo userInfo) {
        UserInfo savedUser = userInfoService.saveUser(userInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    // 更新用户信息
    @PutMapping("/{id}")
    public ResponseEntity<UserInfo> updateUser(@PathVariable Long id, @RequestBody UserInfo userInfo) {
        Optional<UserInfo> existingUser = userInfoService.getUserById(id);
        if (existingUser.isPresent()) {
            userInfo.setId(id);
            UserInfo updatedUser = userInfoService.updateUser(userInfo);
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // 删除用户
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        Optional<UserInfo> existingUser = userInfoService.getUserById(id);
        if (existingUser.isPresent()) {
            userInfoService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}