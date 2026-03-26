package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkEmbeddingRepository extends JpaRepository<KnowledgeChunkEmbedding, Long> {

    List<KnowledgeChunkEmbedding> findByChunk_IdOrderByIdDesc(Long chunkId);
}
