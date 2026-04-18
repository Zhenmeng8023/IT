package com.alikeyou.itmodulesystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "audit_log", schema = "it9_data")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    //主键id，自增
    private Long id;

    @Column(name = "action", nullable = false, length = 50)
    //操作类型，用于标识发生了什么操作
    private String action;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_type", length = 50)
    //目标类型，用于标识操作的对象类型，例如“blog”、“user”、“comment”等等
    private String targetType;

    @Column(name = "target_id")
    //目标ID，用于标识操作的对象ID
    private Long targetId;

    @Column(name = "ip_address", length = 45)
    //IP地址，用于标识操作的IP地址
    private String ipAddress;

    @Lob
    @Column(name = "user_agent")
    //用户代理，用于标识操作的浏览器信息
    private String userAgent;

    @Column(name = "details")
    @JdbcTypeCode(SqlTypes.JSON)
    //详情，存储额外的操作细节
    private Map<String, Object> details;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    //创建时间，记录操作的时间
    private Instant createdAt;

}
