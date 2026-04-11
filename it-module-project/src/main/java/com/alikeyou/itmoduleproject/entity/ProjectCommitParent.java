package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_commit_parent")
public class ProjectCommitParent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "commit_id", nullable = false)
    private Long commitId;
    @Column(name = "parent_commit_id", nullable = false)
    private Long parentCommitId;
    @Column(name = "parent_order", nullable = false)
    private Integer parentOrder;
}
