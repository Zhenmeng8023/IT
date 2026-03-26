package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "knowledge_base_member", schema = "it9_data")
public class KnowledgeBaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 20)
    private RoleCode roleCode;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum RoleCode {
        OWNER, EDITOR, VIEWER
    }
}
