package com.alikeyou.itmoduleblog.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class BlogResponse {
    private Long id;
    private String title;
    private String content;
    private String coverImageUrl;
    private List<String> tags; // 修改为字符串列表
    private AuthorInfo author;
    private ProjectInfo project;
    private String status;
    private Boolean isMarked;
    private Instant publishTime;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer downloadCount;

    @Data
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String avatar;
        private String displayName; // 新增字段
        private String email;      // 新增字段
    }

    @Data
    public static class ProjectInfo {
        private Long id;
        private String name;
        private String description; // 新增字段
        private String category;    // 新增字段
    }
}