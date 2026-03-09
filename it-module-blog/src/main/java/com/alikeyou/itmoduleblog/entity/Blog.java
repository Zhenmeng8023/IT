package com.alikeyou.itmoduleblog.entity;

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

@Getter     //@Getter @Setter：Lombok注解，自动生成getter和setter方法
@Setter
@Entity     //@Entity：标记此类为数据库JPA实体类
@Table(name = "blog", schema = "it_data")   //@Table：标记此类对应的数据库表和scheme
public class Blog {
    @Id         //@Id：标记此属性为数据库主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)     //@GeneratedValue：指定主键生成策略为自增
    @Column(name = "id", nullable = false)      //@Column：指定数据库列名和约束，nullable = false表示不可为空
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob //@Lob：指定此属性为长文本类型，用于存储大文本内容
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "tags")  //tags:博客标签 使用 @JdbcTypeCode(SqlTypes.JSON) 注解将 Map 类型映射为 JSON 格式存储
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tags;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "author_id", nullable = false)
    private com.alikeyou.itmoduleuser.entity.UserInfo author;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "project_id")
    private com.alikeyou.itmoduleblog.entity.Project project;

    @ColumnDefault("'draft'")
    @Lob
    @Column(name = "status")
    private String status;     //博客状态，默认值为'draft'

    @ColumnDefault("0")
    @Column(name = "is_marked")
    private Boolean isMarked;   //博客是否被收藏

    @Column(name = "publish_time")
    private Instant publishTime;   //博客发布时间

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;     //博客创建时间

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;    //博客更新时间

    @ColumnDefault("0")
    @Column(name = "view_count")
    private Integer viewCount;   //博客浏览次数

    @ColumnDefault("0")
    @Column(name = "like_count")
    private Integer likeCount;   //博客点赞次数

    @ColumnDefault("0")
    @Column(name = "collect_count")
    private Integer collectCount;   //博客收藏次数

    @ColumnDefault("0")
    @Column(name = "download_count")
    private Integer downloadCount;   //博客下载次数

}