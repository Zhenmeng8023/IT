package com.alikeyou.itmodulecommon.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "menu", schema = "it_data")
public class Menu {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "path", length = 200)
    private String path;

    @Column(name = "component", length = 200)
    private String component;

    @Column(name = "icon", length = 50)
    private String icon;

    @ColumnDefault("0")
    @Column(name = "sort_order")
    private Integer sortOrder;

    @ColumnDefault("0")
    @Column(name = "is_hidden")
    private Boolean isHidden;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "parent_id")
    private Integer parentId;



}