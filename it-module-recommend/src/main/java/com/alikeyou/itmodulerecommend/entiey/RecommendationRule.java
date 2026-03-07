package com.alikeyou.itmodulerecommend.entiey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "recommendation_rule", schema = "it_data")
public class RecommendationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @ColumnDefault("'content_based'")
    @Lob
    @Column(name = "rule_type")
    private String ruleType;

    @ColumnDefault("1.00")
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @ColumnDefault("1")
    @Column(name = "enabled")
    private Boolean enabled;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

}