package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_template", schema = "it9_data")
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 80, unique = true)
    private String code;

    @ColumnDefault("'system'")
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "title_template", nullable = false, length = 200)
    private String titleTemplate;

    @Lob
    @Column(name = "content_template", nullable = false)
    private String contentTemplate;

    @Column(name = "action_url_template", length = 500)
    private String actionUrlTemplate;

    @ColumnDefault("0")
    @Column(name = "default_priority", nullable = false)
    private Integer defaultPriority;

    @ColumnDefault("1")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "remark", length = 500)
    private String remark;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
