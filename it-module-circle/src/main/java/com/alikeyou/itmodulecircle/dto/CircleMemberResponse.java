package com.alikeyou.itmodulecircle.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CircleMemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Instant joinTime;
    private String status;
    private String role;
}
