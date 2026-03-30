package com.alikeyou.itmodulecommon.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import com.alikeyou.itmodulecommon.entity.UserInfo;

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
    
    /**
     * 账户余额
     */
    private java.math.BigDecimal balance;
    
    /**
     * 是否为 VIP 会员（高级会员）
     */
    private Boolean isVip;
    
    /**
     * VIP 状态：active（有效）、inactive（无效）、expired（过期）
     */
    private String vipStatus;
    
    /**
     * VIP 等级：0-普通用户，1-VIP 会员，2-高级 VIP
     */
    private Integer vipLevel;

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
        
        // 处理账户余额
        this.balance = userInfo.getBalance();
        
        // 处理 VIP 会员信息
        this.isVip = userInfo.getIsPremiumMember() != null && userInfo.getIsPremiumMember();
        
        // 根据高级会员状态和到期时间设置 VIP 状态
        if (!this.isVip) {
            this.vipStatus = "inactive";
            this.vipLevel = 0;
        } else if (userInfo.getPremiumExpiryDate() != null && 
                   userInfo.getPremiumExpiryDate().isBefore(Instant.now())) {
            this.vipStatus = "expired";
            this.vipLevel = 0;
        } else {
            this.vipStatus = "active";
            this.vipLevel = 1; // 默认为普通 VIP
        }
    }
}