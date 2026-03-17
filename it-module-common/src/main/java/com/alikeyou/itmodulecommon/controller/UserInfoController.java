package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.entity.Region;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.dto.UpdateUserDTO;
import java.util.List;
import com.alikeyou.itmodulecommon.dto.UserResponseDTO;
import com.alikeyou.itmodulecommon.dto.ChangePasswordDTO;
import com.alikeyou.itmodulecommon.dto.ChangeEmailDTO;
import com.alikeyou.itmodulecommon.dto.ChangeUsernameDTO;
import com.alikeyou.itmodulecommon.dto.VerifyPasswordDTO;
import com.alikeyou.itmodulecommon.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
        @Parameter(description = "用户ID", required = true)
        @PathVariable Long id) {
        logger.info("Request: GET /api/users/{}", id);
        Optional<UserInfo> userInfo = userInfoService.getUserById(id);
        ResponseEntity<UserResponseDTO> response = userInfo.map(u -> ResponseEntity.ok(new UserResponseDTO(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        logger.info("Response: {} - {}", response.getStatusCode(), userInfo.orElse(null));
        return response;
    }

    // 获取当前用户信息（使用LoginConstant中存储的用户信息）
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取当前用户信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
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
    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "成功创建用户", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
        @Parameter(description = "用户信息", required = true) 
        @RequestBody UserInfo userInfo) {
        logger.info("Request: POST /api/users - {}", userInfo);
        UserInfo savedUser = userInfoService.saveUser(userInfo);
        UserResponseDTO responseDTO = new UserResponseDTO(savedUser);
        ResponseEntity<UserResponseDTO> response = ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        logger.info("Response: {} - {}", response.getStatusCode(), savedUser);
        return response;
    }

    // 更新用户信息
    @Operation(summary = "更新用户信息", description = "根据用户ID更新用户信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功更新用户信息", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long id,
        @Parameter(description = "用户信息", required = true) 
        @RequestBody UserInfo userInfo) {
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
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "成功删除用户"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long id) {
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
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新用户信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
    @PutMapping("/updatemine")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(
        @Parameter(description = "用户信息更新请求", required = true) 
        @RequestBody UpdateUserDTO updateUserDTO) {
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
    
    // 修改密码
    @Operation(summary = "修改密码", description = "当前登录用户修改自己的登录密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功修改密码"),
            @ApiResponse(responseCode = "400", description = "密码验证失败或新密码不符合要求"),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
    @PutMapping("/changepwd")
    public ResponseEntity<Void> changePassword(
        @Parameter(description = "修改密码请求", required = true) 
        @RequestBody ChangePasswordDTO changePasswordDTO) {
        logger.info("Request: PUT /api/users/changepwd");
        Optional<UserInfo> currentUser = userInfoService.getCurrentUser();
        if (currentUser.isPresent()) {
            // 验证新密码和确认密码是否一致
            if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 调用服务修改密码
            boolean success = userInfoService.changePassword(
                currentUser.get().getId(), 
                changePasswordDTO.getOldPassword(), 
                changePasswordDTO.getNewPassword()
            );
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // 修改邮箱
    @Operation(summary = "修改邮箱", description = "当前登录用户修改自己的绑定邮箱")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功修改邮箱"),
            @ApiResponse(responseCode = "400", description = "邮箱格式不正确或验证码无效"),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
    @PutMapping("/changeemail")
    public ResponseEntity<Void> changeEmail(
        @Parameter(description = "修改邮箱请求", required = true) 
        @RequestBody ChangeEmailDTO changeEmailDTO) {
        logger.info("Request: PUT /api/users/changeemail - {}", changeEmailDTO.getNewEmail());
        Optional<UserInfo> currentUser = userInfoService.getCurrentUser();
        if (currentUser.isPresent()) {
            // 调用服务修改邮箱
            boolean success = userInfoService.changeEmail(
                currentUser.get().getId(), 
                changeEmailDTO.getNewEmail()
            );
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // 修改用户名
    @Operation(summary = "修改用户名", description = "当前登录用户修改自己的用户名")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功修改用户名"),
            @ApiResponse(responseCode = "400", description = "用户名不符合要求"),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
    @PutMapping("/changename")
    public ResponseEntity<Void> changeUsername(
        @Parameter(description = "修改用户名请求", required = true) 
        @RequestBody ChangeUsernameDTO changeUsernameDTO) {
        logger.info("Request: PUT /api/users/changename - {}", changeUsernameDTO.getNewUsername());
        Optional<UserInfo> currentUser = userInfoService.getCurrentUser();
        if (currentUser.isPresent()) {
            // 调用服务修改用户名
            boolean success = userInfoService.changeUsername(
                currentUser.get().getId(), 
                changeUsernameDTO.getNewUsername()
            );
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // 验证密码
    @Operation(summary = "验证密码", description = "修改敏感信息前校验当前密码，提升安全性")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码验证成功"),
            @ApiResponse(responseCode = "400", description = "密码验证失败"),
            @ApiResponse(responseCode = "404", description = "用户未登录")
    })
    @PostMapping("/verify-pwd")
    public ResponseEntity<Void> verifyPassword(
        @Parameter(description = "验证密码请求", required = true) 
        @RequestBody VerifyPasswordDTO verifyPasswordDTO) {
        logger.info("Request: POST /api/users/verify-pwd");
        Optional<UserInfo> currentUser = userInfoService.getCurrentUser();
        if (currentUser.isPresent()) {
            // 调用服务验证密码
            boolean success = userInfoService.verifyPassword(
                currentUser.get().getId(), 
                verifyPasswordDTO.getPassword()
            );
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // 绑定第三方账号
    @Operation(summary = "绑定第三方账号", description = "绑定微信/QQ等第三方登录账号")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功绑定第三方账号"),
            @ApiResponse(responseCode = "400", description = "绑定失败"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PostMapping("/{id}/bind-third")
    public ResponseEntity<Void> bindThirdParty(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long id,
        @Parameter(description = "第三方类型", required = true) 
        @RequestParam String type,
        @Parameter(description = "第三方ID", required = true) 
        @RequestParam String thirdPartyId) {
        logger.info("Request: POST /api/users/{}/bind-third - type: {}, thirdPartyId: {}", id, type, thirdPartyId);
        
        // 调用服务绑定第三方账号
        boolean success = userInfoService.bindThirdParty(id, type, thirdPartyId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 获取用户公开信息
    @Operation(summary = "获取用户公开信息", description = "查看其他用户公开信息（屏蔽敏感字段）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户公开信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}/public")
    public ResponseEntity<UserResponseDTO> getPublicUserInfo(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long id) {
        logger.info("Request: GET /api/users/{}/public", id);
        Optional<UserInfo> userInfo = userInfoService.getPublicUserInfo(id);
        
        // 这里可以对userInfo进行处理，屏蔽敏感字段
        ResponseEntity<UserResponseDTO> response = userInfo.map(u -> {
            // 屏蔽敏感字段
            u.setPasswordHash(null);
            u.setEmail(null);
            u.setIdentityCard(null);
            return ResponseEntity.ok(new UserResponseDTO(u));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        
        logger.info("Response: {} - {}", response.getStatusCode(), userInfo.orElse(null));
        return response;
    }

    // 为用户分配角色
    @Operation(summary = "为用户分配角色", description = "为指定用户分配角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功为用户分配角色"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{userId}/assign-roles")
    public ResponseEntity<Void> assignRoles(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long userId,
        @Parameter(description = "角色ID列表", required = true) 
        @RequestBody List<Integer> roleIds) {
        logger.info("Request: PUT /api/users/{}/assign-roles - {}", userId, roleIds);
        try {
            userInfoService.assignRoles(userId, roleIds);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 获取用户的角色列表
    @Operation(summary = "获取用户的角色列表", description = "获取指定用户拥有的所有角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户角色列表"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<Role>> getUserRoles(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long userId) {
        logger.info("Request: GET /api/users/{}/roles", userId);
        try {
            List<Role> roles = userInfoService.getUserRoles(userId);
            return ResponseEntity.ok(roles);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 获取用户的菜单权限列表
    @Operation(summary = "获取用户的菜单权限列表", description = "获取指定用户通过角色继承而来的所有菜单权限")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户菜单权限列表"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{userId}/menus")
    public ResponseEntity<List<Menu>> getUserMenus(
        @Parameter(description = "用户ID", required = true) 
        @PathVariable Long userId) {
        logger.info("Request: GET /api/users/{}/menus", userId);
        try {
            List<Menu> menus = userInfoService.getUserMenus(userId);
            return ResponseEntity.ok(menus);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 获取所有用户所有信息（后端使用）
    @Operation(summary = "获取所有用户所有信息", description = "获取所有用户的完整信息，用于后端管理显示")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取所有用户信息")
    })
    @GetMapping
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        logger.info("Request: GET /api/users");
        List<UserInfo> users = userInfoService.getAllUsers();
        // 清除敏感字段，如密码哈希
        for (UserInfo user : users) {
            user.setPasswordHash(null);
        }
        ResponseEntity<List<UserInfo>> response = ResponseEntity.ok(users);
        logger.info("Response: {} - {} users", response.getStatusCode(), users.size());
        return response;
    }
}