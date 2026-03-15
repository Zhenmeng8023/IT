package com.alikeyou.itmoduleuser.service;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.entity.Permission;
import com.alikeyou.itmoduleuser.repository.UserInfoRepository;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserInfoService {
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    // 根据ID获取用户信息
    public Optional<UserInfo> getUserById(Long id) {
        return userInfoRepository.findByIdWithAssociations(id);
    }
    
    // 根据用户名获取用户信息
    public Optional<UserInfo> getUserByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }
    
    // 根据邮箱获取用户信息
    public Optional<UserInfo> getUserByEmail(String email) {
        return userInfoRepository.findByEmail(email);
    }
    
    // 根据LoginConstant中存储的用户信息获取当前用户
    public Optional<UserInfo> getCurrentUser() {
        // 优先使用userId查询
        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            return getUserById(userId);
        }
        
        // 其次使用username查询
        String username = LoginConstant.getUsername();
        if (username != null) {
            return getUserByUsername(username);
        }
        
        // 最后使用email查询
        String email = LoginConstant.getEmail();
        if (email != null) {
            return getUserByEmail(email);
        }
        
        return Optional.empty();
    }
    
    // 保存用户信息
    public UserInfo saveUser(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }
    
    // 更新用户信息
    public UserInfo updateUser(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }
    
    // 删除用户
    public void deleteUser(Long id) {
        userInfoRepository.deleteById(id);
    }
    
    // 修改密码
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加密码验证逻辑，暂时假设密码正确
            user.setPasswordHash(newPassword);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 修改邮箱
    public boolean changeEmail(Long userId, String newEmail) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            user.setEmail(newEmail);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 修改用户名
    public boolean changeUsername(Long userId, String newUsername) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            user.setUsername(newUsername);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 验证密码
    public boolean verifyPassword(Long userId, String password) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加密码验证逻辑，暂时假设密码正确
            return true;
        }
        return false;
    }
    
    // 绑定第三方账号
    public boolean bindThirdParty(Long userId, String thirdPartyType, String thirdPartyId) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加第三方账号绑定逻辑
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 获取用户公开信息
    public Optional<UserInfo> getPublicUserInfo(Long userId) {
        return userInfoRepository.findByIdWithAssociations(userId);
    }
    
    // 为用户分配角色
    public void assignRoles(Long userId, List<Integer> roleIds) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            // 用户只能有一个角色，所以只取第一个角色ID
            if (!roleIds.isEmpty()) {
                Integer roleId = roleIds.get(0);
                // 验证角色是否存在
                Optional<Role> roleOptional = roleRepository.findById(roleId);
                if (roleOptional.isPresent()) {
                    user.setRoleId(roleId);
                    userInfoRepository.save(user);
                } else {
                    throw new RuntimeException("Role not found with id: " + roleId);
                }
            }
        } else {
            throw new RuntimeException("User not found with id: " + userId);
        }
    }

    // 获取用户的角色列表
    public List<Role> getUserRoles(Long userId) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            Integer roleId = user.getRoleId();
            if (roleId != null) {
                Optional<Role> roleOptional = roleRepository.findById(roleId);
                if (roleOptional.isPresent()) {
                    return List.of(roleOptional.get());
                }
            }
        } else {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return List.of();
    }

    // 获取用户的权限列表
    public List<Permission> getUserPermissions(Long userId) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            Integer roleId = user.getRoleId();
            if (roleId != null) {
                Optional<Role> roleOptional = roleRepository.findById(roleId);
                if (roleOptional.isPresent()) {
                    Role role = roleOptional.get();
                    return role.getPermissions().stream().collect(java.util.stream.Collectors.toList());
                }
            }
        } else {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return List.of();
    }
}