/**
 * 项目成员实体类
 * 对应数据库中的project_member表，存储项目成员的信息
 */
package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目成员实体类
 * 对应数据库中的project_member表，存储项目成员的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_member")
public class ProjectMember {

    /**
     * 成员ID，自增主键
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
     * 用户ID，不能为空
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 角色，不能为空，最大长度20，对应ProjectMemberRoleEnum枚举
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /**
     * 状态，不能为空，最大长度20，对应ProjectMemberStatusEnum枚举
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 加入时间，不能为空
     */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 更新时间，不能为空
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 持久化前的处理
     * 设置默认时间戳
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新前的处理
     * 更新时间戳
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}