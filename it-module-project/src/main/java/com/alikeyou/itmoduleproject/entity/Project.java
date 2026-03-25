/**
 * 项目实体类
 * 对应数据库中的project表，存储项目的基本信息
 */
package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目实体类
 * 对应数据库中的project表，存储项目的基本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project")
public class Project {

    /**
     * 项目ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目名称，不能为空，最大长度100
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 项目描述，文本类型
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * 项目分类，最大长度50
     */
    @Column(length = 50)
    private String category;

    /**
     * 项目大小，单位MB，精度10，小数位2
     */
    @Column(name = "size_mb", precision = 10, scale = 2)
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
     * 项目作者ID，不能为空
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * 项目状态，最大长度20，对应ProjectStatusEnum枚举
     */
    @Column(length = 20)
    private String status;

    /**
     * 项目标签，JSON格式存储
     */
    @Column(columnDefinition = "json")
    private String tags;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 模板ID
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * 项目可见性，最大长度20，对应ProjectVisibilityEnum枚举
     */
    @Column(length = 20)
    private String visibility;

    /**
     * 持久化前的处理
     * 设置默认值和时间戳
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (sizeMb == null) {
            sizeMb = BigDecimal.ZERO;
        }
        if (stars == null) {
            stars = 0;
        }
        if (downloads == null) {
            downloads = 0;
        }
        if (views == null) {
            views = 0;
        }
    }

    /**
     * 更新前的处理
     * 更新时间戳
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}