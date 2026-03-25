/**
 * 项目详情VO
 * 用于返回项目的详细信息
 */
package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目详情VO
 * 用于返回项目的详细信息
 */
@Data
@Builder
public class ProjectDetailVO {
    /**
     * 项目ID
     */
    private Long id;
    /**
     * 项目名称
     */
    private String name;
    /**
     * 项目描述
     */
    private String description;
    /**
     * 项目分类
     */
    private String category;
    /**
     * 项目大小，单位MB
     */
    private BigDecimal sizeMb;
    /**
     * 项目星级
     */
    private Integer stars;
    /**
     * 项目下载次数
     */
    private Integer downloads;
    /**
     * 项目查看次数
     */
    private Integer views;
    /**
     * 项目作者ID
     */
    private Long authorId;
    /**
     * 项目状态
     */
    private String status;
    /**
     * 项目标签
     */
    private String tags;
    /**
     * 模板ID
     */
    private Long templateId;
    /**
     * 项目可见性
     */
    private String visibility;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 项目成员列表
     */
    private List<ProjectMemberVO> members;
    /**
     * 项目任务列表
     */
    private List<ProjectTaskVO> tasks;
    /**
     * 项目文件列表
     */
    private List<ProjectFileVO> files;
}