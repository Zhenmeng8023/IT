package com.alikeyou.itmodulecircle.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class CircleManageCircleItemResponse {

    private Long id;

    private String name;

    private String description;

    /**
     * 生命周期状态：pending/approved/close/rejected
     */
    private String type;

    /**
     * 兼容旧管理页状态：pending/normal/closed/violation
     */
    private String status;

    private String visibility;

    /**
     * visibility 的兼容别名
     */
    private String privacy;

    private Integer maxMembers;

    private Long creatorId;

    /**
     * 兼容旧管理页字段，直接输出字符串
     */
    private String creator;

    private String creatorName;

    private String creatorAvatar;

    private String creatorAvatarUrl;

    /**
     * 新管理页可用的创建者对象
     */
    private CircleCreatorInfo creatorInfo;

    private Long memberCount;

    private Long activeMemberCount;

    private Long postCount;

    private Long todayActive;

    private Boolean isRecommended;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * createdAt 的兼容别名
     */
    private Instant createTime;
}

