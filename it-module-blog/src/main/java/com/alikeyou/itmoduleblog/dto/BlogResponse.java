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
    private String previewContent;
    private String coverImageUrl;
    private List<String> tags;
    private List<Long> tagIds;
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
    private Integer reportCount;
    private String auditReason;
    private String rejectReason;
    private Integer price;
    private Boolean locked;
    private String lockType;
    private Boolean hasAccess;
    private Boolean hasPurchased;
    private Boolean isVipUser;

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
