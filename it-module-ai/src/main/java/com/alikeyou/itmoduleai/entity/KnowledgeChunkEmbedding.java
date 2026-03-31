package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "knowledge_chunk_embedding", schema = "it9_data")
public class KnowledgeChunkEmbedding {

    public enum Status {
        ACTIVE,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private KnowledgeChunk chunk;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "dimension")
    private Integer dimension;

    @Column(name = "vector_ref", length = 255)
    private String vectorRef;

    @Lob
    @Column(name = "vector_payload")
    private String vectorPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
