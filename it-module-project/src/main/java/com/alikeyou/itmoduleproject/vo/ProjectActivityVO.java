package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectActivityVO {
    private Long id;
    private Long projectId;
    private Long operatorId;
    private String operatorName;
    private String operatorAvatar;
    private String action;
    private String actionLabel;
    private String actionTagType;
    private String targetType;
    private Long targetId;
    private String content;
    private LocalDateTime createdAt;
    private String groupDay;
}
