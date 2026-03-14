package com.alikeyou.itmodulecircle.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
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
@Table(name = "circle", schema = "it_data")
public class Circle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ColumnDefault("'public'")
    @Column(name = "type", length = 20)
    private String type;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @ColumnDefault("'public'")
    @Column(name = "visibility")
    private String visibility;

    @ColumnDefault("500")
    @Column(name = "max_members")
    private Integer maxMembers;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "circle")
    private Set<CircleMember> circleMembers = new LinkedHashSet<>();

}