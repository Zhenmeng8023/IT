package com.alikeyou.itmoduleblog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * BlogUser 实体类
 * 用于管理博客系统的用户信息，替代Blog实体中的硬编码作者信息
 * 与Blog实体建立多对一关联关系
 */
@Getter
@Setter
@Entity
@Table(name = "blog_user", schema = "it_data")
public class BlogUser {

    /**
     * 用户ID - 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户名 - 唯一且不可为空
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * 用户邮箱 - 唯一且可为空
     */
    @Column(name = "email", unique = true, length = 255)
    private String email;

    /**
     * 用户头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 用户显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 用户简介
     */
    @Lob
    @Column(name = "bio")
    private String bio;

    /**
     * 用户标签/分类信息 - 使用JSON格式存储
     */
    @Column(name = "tags")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tags;

    /**
     * 用户状态
     * active: 活跃, inactive: 非活跃, banned: 封禁
     */
    @ColumnDefault("'active'")
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 是否为管理员
     */
    @ColumnDefault("false")
    @Column(name = "is_admin")
    private Boolean isAdmin;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * 创建时间 - 自动设置
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * 更新时间 - 自动更新
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * 默认构造函数
     */
    public BlogUser() {
        // 设置默认值
        this.status = "active";
        this.isAdmin = false;
    }

    /**
     * 带参数的构造函数
     */
    public BlogUser(String username, String email) {
        this();
        this.username = username;
        this.email = email;
    }

    /**
     * 带详细信息的构造函数
     */
    public BlogUser(String username, String email, String displayName, String avatarUrl) {
        this(username, email);
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String toString() {
        return "BlogUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + (email != null ? email.substring(0, Math.min(email.length(), 3)) + "..." : "null") + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", isAdmin=" + isAdmin +
                '}';
    }

    /**
     * 判断用户是否活跃
     */
    public boolean isActive() {
        return "active".equals(this.status);
    }

    /**
     * 判断用户是否为管理员
     */
    public boolean isAdminUser() {
        return Boolean.TRUE.equals(this.isAdmin);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }
}