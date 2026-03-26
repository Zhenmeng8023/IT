package com.alikeyou.itmoduleblog.entity;

// \it-module-blog\src\main\java\com\alikeyou\itmoduleblog\entity\ViewLog.java
// ViewLog 实体类用于记录用户的浏览行为，映射到数据库的 view_log 表，主要用于统计和分析用户的访问情况

// 移除外部模块依赖
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "view_log", schema = "it9_data")
public class ViewLog {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_username", length = 100)
    private String userUsername;

    @Lob
    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Lob
    @Column(name = "user_agent")
    private String userAgent;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

}