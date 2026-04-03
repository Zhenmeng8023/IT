package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskLogVO {
    private Long id;
    private Long taskId;
    private Long operatorId;
    private String operatorName;
    private String operatorAvatar;
    private String action;
    private String actionLabel;
    private String actionTagType;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String oldValueDisplay;
    private String newValueDisplay;
    private LocalDateTime createdAt;
    private String groupDay;
}
