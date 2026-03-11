package com.alikeyou.itmoduleblog.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
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
@Table(name = "blog", schema = "it_data")
public class Blog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "tags")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tags;
    /**
     * 作者信息 - 使用common模块中的UserInfo实体关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "blog_author_fk"),
            referencedColumnName = "id")
    private UserInfo author;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "project_id")
    private Project project;

    @ColumnDefault("'draft'")
    @Lob
    @Column(name = "status")
    private String status;

    @ColumnDefault("0")
    @Column(name = "is_marked")
    private Boolean isMarked;

    @Column(name = "publish_time")
    private Instant publishTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ColumnDefault("0")
    @Column(name = "view_count")
    private Integer viewCount;

    @ColumnDefault("0")
    @Column(name = "like_count")
    private Integer likeCount;

    @ColumnDefault("0")
    @Column(name = "collect_count")
    private Integer collectCount;

    @ColumnDefault("0")
    @Column(name = "download_count")
    private Integer downloadCount;
}