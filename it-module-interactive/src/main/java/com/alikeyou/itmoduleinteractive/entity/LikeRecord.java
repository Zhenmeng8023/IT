package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "like_record", schema = "it_data", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_target", columnNames = {"user_id", "target_type", "target_id"})
})
public class LikeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false, columnDefinition = "enum('blog','comment')")
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

}