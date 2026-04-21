package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class BlogDashboardActivityDTO {
    private String title;
    private String desc;
    private Instant time;
    private String level;
    private String category;
}
