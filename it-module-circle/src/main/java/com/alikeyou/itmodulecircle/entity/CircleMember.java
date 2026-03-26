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
@Table(name = "circle_member", schema = "it9_data")
public class CircleMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo user;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "join_time")
    private Instant joinTime;

    @ColumnDefault("'active'")
    @Lob
    @Column(name = "status")
    private String status;

    @ColumnDefault("'member'")
    @Lob
    @Column(name = "role")
    private String role;

}