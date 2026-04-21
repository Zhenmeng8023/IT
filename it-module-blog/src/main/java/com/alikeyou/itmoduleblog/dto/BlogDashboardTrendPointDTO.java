package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

@Data
public class BlogDashboardTrendPointDTO {
    private String statDate;
    private Long createdCount;
    private Long publishedCount;
    private Long viewCount;
    private Long reportCount;
}
