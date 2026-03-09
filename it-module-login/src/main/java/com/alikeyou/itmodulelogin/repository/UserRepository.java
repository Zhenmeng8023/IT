package com.alikeyou.itmodulelogin.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.alikeyou.itmodulelogin.entity.LoginUser;
import com.alikeyou.itmodulelogin.utils.PasswordEncoder;

public class UserRepository {
    // 模拟数据库存储
    private static final List<LoginUser> users = new ArrayList<>();

    static {
        // 初始化默认用户
        LoginUser admin = new LoginUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(PasswordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setEmail("admin@example.com");
        admin.setEnabled(true);
        users.add(admin);

        LoginUser user = new LoginUser();
        user.setId(2L);
        user.setUsername("user");
        user.setPassword(PasswordEncoder.encode("user123"));
        user.setNickname("普通用户");
        user.setEmail("user@example.com");
        user.setEnabled(true);
        users.add(user);
    }

    public Optional<LoginUser> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    public void save(LoginUser user) {
        users.add(user);
    }

    public List<LoginUser> findAll() {
        return users;
    }
}