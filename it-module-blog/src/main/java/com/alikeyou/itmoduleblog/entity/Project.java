package com.alikeyou.itmoduleblog.entity;

import com.alikeyou.itmoduleuser.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "project", schema = "it_data")
public class Project {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @ColumnDefault("0.00")
    @Column(name = "size_mb", precision = 10, scale = 2)
    private BigDecimal sizeMb;

    @ColumnDefault("0")
    @Column(name = "stars")
    private Integer stars;

    @ColumnDefault("0")
    @Column(name = "downloads")
    private Integer downloads;

    @ColumnDefault("0")
    @Column(name = "views")
    private Integer views;

    @ColumnDefault("'draft'")
    @Lob
    @Column(name = "status")
    private String status;

    @Column(name = "tags")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tags;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "author_id", nullable = false)
    private UserInfo author;

    @OneToMany(mappedBy = "project")
    private Set<Blog> blogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "project")
    private Set<ProjectFile> projectFiles = new LinkedHashSet<>();

}