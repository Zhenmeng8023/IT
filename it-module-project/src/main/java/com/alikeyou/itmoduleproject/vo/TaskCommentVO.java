package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskCommentVO {
    private Long id;
    private Long taskId;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private Long parentCommentId;
    private String content;
    private String status;
    private Boolean canDelete;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TaskCommentVO> replies;
}
