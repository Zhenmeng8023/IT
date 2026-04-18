package com.alikeyou.itmodulecircle.dto;

import lombok.Data;

@Data
public class CircleManageStatsResponse {

    private Long totalCircles;

    private Long totalMembers;

    private Long activeMembers;

    private Long totalPosts;

    /**
     * 兼容旧管理页字段，语义上等同于活跃成员数
     */
    private Long todayActive;
}

