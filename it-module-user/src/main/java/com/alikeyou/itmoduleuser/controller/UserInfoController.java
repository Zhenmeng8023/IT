package com.alikeyou.itmoduleuser.controller;

import com.alikeyou.itmodulecommon.entity.Region;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.dto.UpdateUserDTO;
import com.alikeyou.itmoduleuser.dto.UserResponseDTO;
import com.alikeyou.itmoduleuser.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserInfoController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);
    
    @Autowired
    private UserInfoService userInfoService;
    
    // 获取用户信息
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        logger.info("Request: GET /api/users/{}", id);
        Optional<UserInfo> userInfo = userInfoService.getUserById(id);
        ResponseEntity<UserResponseDTO> response = userInfo.map(u -> ResponseEntity.ok(new UserResponseDTO(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        logger.info("Response: {} - {}", response.getStatusCode(), userInfo.orElse(null));
        return response;
    }
    
    // 获取当前用户信息（使用LoginConstant中存储的用户信息）
    @GetMapping("/current")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        logger.info("Request: GET /api/users/current");
        Optional<UserInfo> userInfo = userInfoService.getCurrentUser();
        ResponseEntity<UserResponseDTO> response = userInfo.map(u -> ResponseEntity.ok(new UserResponseDTO(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        logger.info("Response: {} - {}", response.getStatusCode(), userInfo.orElse(null));
        return response;
    }
    
    // 创建用户
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserInfo userInfo) {
        logger.info("Request: POST /api/users - {}", userInfo);
        UserInfo savedUser = userInfoService.saveUser(userInfo);
        UserResponseDTO responseDTO = new UserResponseDTO(savedUser);
        ResponseEntity<UserResponseDTO> response = ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        logger.info("Response: {} - {}", response.getStatusCode(), savedUser);
        return response;
    }
    
    // 更新用户信息
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserInfo userInfo) {
        logger.info("Request: PUT /api/users/{} - {}", id, userInfo);
        Optional<UserInfo> existingUser = userInfoService.getUserById(id);
        ResponseEntity<UserResponseDTO> response;
        if (existingUser.isPresent()) {
            userInfo.setId(id);
            UserInfo updatedUser = userInfoService.updateUser(userInfo);
            // 重新获取用户信息，确保关联数据被加载
            Optional<UserInfo> refreshedUser = userInfoService.getUserById(updatedUser.getId());
            response = refreshedUser.map(u -> ResponseEntity.ok(new UserResponseDTO(u)))
                    .orElseGet(() -> ResponseEntity.ok(new UserResponseDTO(updatedUser)));
            logger.info("Response: {} - {}", response.getStatusCode(), updatedUser);
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            logger.info("Response: {}", response.getStatusCode());
        }
        return response;
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("Request: DELETE /api/users/{}", id);
        Optional<UserInfo> existingUser = userInfoService.getUserById(id);
        ResponseEntity<Void> response;
        if (existingUser.isPresent()) {
            userInfoService.deleteUser(id);
            response = ResponseEntity.noContent().build();
            logger.info("Response: {}", response.getStatusCode());
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            logger.info("Response: {}", response.getStatusCode());
        }
        return response;
    }

    // 更新当前用户信息
    @PutMapping("/updatemine")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(@RequestBody UpdateUserDTO updateUserDTO) {
        logger.info("Request: PUT /api/users/updatemine - {}", updateUserDTO);
        Optional<UserInfo> currentUser = userInfoService.getCurrentUser();
        ResponseEntity<UserResponseDTO> response;
        if (currentUser.isPresent()) {
            UserInfo existingUser = currentUser.get();
            // 只更新允许修改的字段
            // 保留不可修改的字段
            existingUser.setPhone(updateUserDTO.getPhone());
            existingUser.setGender(updateUserDTO.getGender());
            existingUser.setBio(updateUserDTO.getBio());
            existingUser.setAvatarUrl(updateUserDTO.getAvatarUrl());
            existingUser.setNickname(updateUserDTO.getNickname());
            existingUser.setIdentityCard(updateUserDTO.getIdentityCard());
            
            // 处理生日字段
            if (updateUserDTO.getBirthday() != null && !updateUserDTO.getBirthday().isEmpty()) {
                try {
                    existingUser.setBirthday(java.time.LocalDate.parse(updateUserDTO.getBirthday()));
                } catch (Exception e) {
                    logger.error("Invalid birthday format: {}", updateUserDTO.getBirthday());
                }
            }
            
            // 处理地区关联关系
            if (updateUserDTO.getRegionId() != null && !updateUserDTO.getRegionId().isEmpty()) {
                try {
                    Long regionId = Long.parseLong(updateUserDTO.getRegionId());
                    Region region = new Region();
                    region.setId(regionId);
                    existingUser.setRegion(region);
                } catch (NumberFormatException e) {
                    logger.error("Invalid regionId format: {}", updateUserDTO.getRegionId());
                }
            }
            
            // 处理作者标签关联关系
            if (updateUserDTO.getAuthorTagId() != null && !updateUserDTO.getAuthorTagId().isEmpty()) {
                try {
                    Long authorTagId = Long.parseLong(updateUserDTO.getAuthorTagId());
                    Tag tag = new Tag();
                    tag.setId(authorTagId);
                    existingUser.setAuthorTag(tag);
                } catch (NumberFormatException e) {
                    logger.error("Invalid authorTagId format: {}", updateUserDTO.getAuthorTagId());
                }
            }
            
            // TODO: 处理标签的关联关系
            // 这里需要根据前端提交的 tags 数据，更新用户与标签的关联
            
            // 更新用户信息
            UserInfo updatedUser = userInfoService.updateUser(existingUser);
            // 重新获取用户信息，确保关联数据被加载
            Optional<UserInfo> refreshedUser = userInfoService.getUserById(updatedUser.getId());
            response = refreshedUser.map(u -> ResponseEntity.ok(new UserResponseDTO(u)))
                    .orElseGet(() -> ResponseEntity.ok(new UserResponseDTO(updatedUser)));
            logger.info("Response: {} - {}", response.getStatusCode(), updatedUser);
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            logger.info("Response: {}", response.getStatusCode());
        }
        return response;
    }
}