package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseMemberRepository extends JpaRepository<KnowledgeBaseMember, Long> {

    List<KnowledgeBaseMember> findByKnowledgeBase_IdOrderByIdAsc(Long knowledgeBaseId);

    boolean existsByKnowledgeBase_IdAndUserId(Long knowledgeBaseId, Long userId);

    Optional<KnowledgeBaseMember> findByKnowledgeBase_IdAndUserId(Long knowledgeBaseId, Long userId);

    void deleteByKnowledgeBase_IdAndUserId(Long knowledgeBaseId, Long userId);
}