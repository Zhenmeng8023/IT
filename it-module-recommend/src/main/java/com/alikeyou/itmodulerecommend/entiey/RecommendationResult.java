package com.alikeyou.itmodulerecommend.entiey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "recommendation_result", schema = "it_data")
public class RecommendationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private com.alikeyou.itmoduleuser.entity.UserInfo user;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion;

    @Column(name = "recommended_items", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> recommendedItems;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "generated_at")
    private Instant generatedAt;

    @ColumnDefault("0")
    @Column(name = "consumed")
    private Boolean consumed;

    @Column(name = "consumed_at")
    private Instant consumedAt;

}