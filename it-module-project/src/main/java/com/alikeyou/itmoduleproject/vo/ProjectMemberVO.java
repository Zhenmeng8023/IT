package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectMemberVO {
    private Long id;
    private Long projectId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
