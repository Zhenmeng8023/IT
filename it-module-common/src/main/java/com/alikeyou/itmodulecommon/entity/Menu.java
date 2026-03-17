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

    @Column(name = "permission_id")
    private Integer permissionId;

    @ManyToOne
    @JoinColumn(name = "permission_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Permission permission;

    // 菜单类型：menu 或 button
    @Transient
    private String type;

    // 获取菜单类型
    public String getType() {
        if (path == null && component == null) {
            return "button";
        } else {
            return "menu";
        }
    }



}