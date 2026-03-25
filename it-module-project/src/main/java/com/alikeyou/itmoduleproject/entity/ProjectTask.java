/**
 * 项目任务实体类
 * 对应数据库中的project_task表，存储项目任务的信息
 */
package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目任务实体类
 * 对应数据库中的project_task表，存储项目任务的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_task")
public class ProjectTask {

    /**
     * 任务ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目ID，不能为空
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 任务标题，不能为空，最大长度255
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 任务描述，文本类型
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * 任务状态，最大长度20
     */
    @Column(length = 20)
    private String status;

    /**
     * 任务优先级，最大长度20
     */
    @Column(length = 20)
    private String priority;

    /**
     * 任务负责人ID
     */
    @Column(name = "assignee_id")
    private Long assigneeId;

    /**
     * 任务截止日期
     */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 任务完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
     * 持久化前的处理
     * 设置默认时间戳
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