package com.alikeyou.itmodulecircle.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class CircleManagePostResponse {

    private Long id;

    private Long circleId;

    private Long postId;

    private Long authorId;

    /**
     * 兼容旧管理页，使用字符串作者名
     */
    private String author;

    /**
     * 新管理页读取的作者名称
     */
    private String authorName;

    private String authorAvatar;

    private String authorAvatarUrl;

    private String title;

    private String content;

    private String status;

    private Integer viewCount;

    private Long commentCount;

    private Integer likes;

    private Instant createdAt;

    /**
     * createdAt 的兼容别名
     */
    private Instant createTime;
}

