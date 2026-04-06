package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

@Data
public class BlogAdminStatsVO {
    private Long totalBlogs;
    private Long pendingBlogs;
    private Long publishedBlogs;
    private Long rejectedBlogs;
    private Long pendingReports;
    private Long todayReports;
    private Long todayViews;
}
