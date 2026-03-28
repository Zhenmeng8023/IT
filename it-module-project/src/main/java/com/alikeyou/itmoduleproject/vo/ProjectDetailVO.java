package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectDetailVO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal sizeMb;
    private Integer stars;
    private Boolean starred;
    private Integer downloads;
    private Integer views;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String status;
    private String tags;
    private Long templateId;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List members;
    private List tasks;
    private List files;
}
