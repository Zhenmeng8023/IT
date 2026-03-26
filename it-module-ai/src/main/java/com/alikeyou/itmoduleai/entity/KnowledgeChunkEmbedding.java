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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private KnowledgeChunk chunk;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "vector_payload", columnDefinition = "json")
    private String vectorPayload;

    @Column(name = "vector_ref", length = 255)
    private String vectorRef;

    @Column(name = "dimension")
    private Integer dimension;

    @Column(name = "created_at")
    private Instant createdAt;
}
