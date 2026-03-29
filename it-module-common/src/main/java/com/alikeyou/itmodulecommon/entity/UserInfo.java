package com.alikeyou.itmodulecommon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "user_info", schema = "it9_data")
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "identity_card", length = 18)
    private String identityCard;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ColumnDefault("0")
    @Column(name = "login_count")
    private Integer loginCount;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Lob
    @Column(name = "bio")
    private String bio;

    @Column(name = "nickname", length = 100)
    private String nickname;

    // 角色ID
    @Column(name = "role_id")
    private Integer roleId;

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    // 地区关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    // 作者标签关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_tag_id")
    private Tag authorTag;
}