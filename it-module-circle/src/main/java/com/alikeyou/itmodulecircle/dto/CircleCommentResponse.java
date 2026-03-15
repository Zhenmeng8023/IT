package com.alikeyou.itmodulecircle.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CircleCommentResponse {
    private Long id;
    private String content;
    private Long parentCommentId;
    private Long postId;
    private Long circleId;
    private AuthorInfo author;
    private Integer likes;
    private Instant createdAt;
    private String status;
    private Long replyCount;

    @Getter
    @Setter
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
    }
}
