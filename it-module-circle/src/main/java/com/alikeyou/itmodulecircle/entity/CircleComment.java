package com.alikeyou.itmodulecircle.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
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
@Table(name = "circle_comment", schema = "it_data")
public class CircleComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "circle_comment_author_fk"))
    private UserInfo author;

    @Column(name = "circle_id", nullable = false)
    private Long circleId;

    @Column(name = "post_id", nullable = true)
    private Long postId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @ColumnDefault("0")
    @Column(name = "likes", nullable = false)
    private Integer likes = 0;

    @ColumnDefault("'normal'")
    @Column(name = "status", length = 20)
    private String status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_circle_comment_circle"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Circle circle;
}
