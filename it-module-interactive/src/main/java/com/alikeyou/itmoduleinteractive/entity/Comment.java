package com.alikeyou.itmoduleinteractive.entity;

// 移除外部模块依赖
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "comment", schema = "it_data")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "parent_comment_id")
    @JsonBackReference  // 添加这个注解
    private Comment parentComment;
    
    // 父评论ID，用于序列化时返回
    @JsonProperty("parentCommentId")
    public Long getParentCommentId() {
        return parentComment != null ? parentComment.getId() : null;
    }

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @ColumnDefault("0")
    @Column(name = "likes")
    private Integer likes;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("'normal'")
    @Column(name = "status", columnDefinition = "enum('normal','hidden','deleted') default 'normal'")
    private String status;

    @OneToMany(mappedBy = "parentComment")
    @JsonManagedReference  // 添加这个注解
    private Set<Comment> comments = new LinkedHashSet<>();

}