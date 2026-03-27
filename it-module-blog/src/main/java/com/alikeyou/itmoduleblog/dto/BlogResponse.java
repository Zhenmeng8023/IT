package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class BlogResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private List<String> tags;
    private AuthorInfo author;
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
        private String nickname;
        private String avatar;
        private String displayName;
        private String email;
    }
}