package com.alikeyou.itmodulecommon.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthday;
    private String status;
    private String identityCard;
    private Instant lastActiveAt;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Integer loginCount;
    private String avatarUrl;
    private String bio;
    private String nickname;
    private Integer roleId;
    private Long regionId;
    private String regionName;
    private Long authorTagId;
    private String authorTagName;

    public UserResponseDTO() {
    }

    public UserResponseDTO(com.alikeyou.itmodulecommon.entity.UserInfo userInfo) {
        this.id = userInfo.getId();
        this.username = userInfo.getUsername();
        this.email = userInfo.getEmail();
        this.phone = userInfo.getPhone();
        this.gender = userInfo.getGender();
        this.birthday = userInfo.getBirthday();
        this.status = userInfo.getStatus();
        this.identityCard = userInfo.getIdentityCard();
        this.lastActiveAt = userInfo.getLastActiveAt();
        this.createdAt = userInfo.getCreatedAt();
        this.lastLoginAt = userInfo.getLastLoginAt();
        this.loginCount = userInfo.getLoginCount();
        this.avatarUrl = userInfo.getAvatarUrl();
        this.bio = userInfo.getBio();
        this.nickname = userInfo.getNickname();
        this.roleId = userInfo.getRoleId();

        // 处理地区信息
        if (userInfo.getRegion() != null) {
            this.regionId = userInfo.getRegion().getId();
            this.regionName = userInfo.getRegion().getName();
        }

        // 处理作者标签信息
        if (userInfo.getAuthorTag() != null) {
            this.authorTagId = userInfo.getAuthorTag().getId();
            this.authorTagName = userInfo.getAuthorTag().getName();
        }
    }
}