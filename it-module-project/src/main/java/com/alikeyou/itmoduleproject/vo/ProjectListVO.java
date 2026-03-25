package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectListVO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal sizeMb;
    private Integer stars;
    private Integer downloads;
    private Integer views;
    private Long authorId;
    private String status;
    private String tags;
    private Long templateId;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
