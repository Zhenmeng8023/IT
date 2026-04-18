package com.alikeyou.itmodulecircle.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CircleMemberResponse {
    private Long id;
    private Long circleId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    /**
     * avatarUrl 的兼容别名，避免管理端旧页面丢头像
     */
    private String avatar;
    private Instant joinTime;
    private Instant lastActive;
    private String status;
    private String role;
}
