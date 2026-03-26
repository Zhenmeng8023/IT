package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseMemberRepository extends JpaRepository<KnowledgeBaseMember, Long> {

    List<KnowledgeBaseMember> findByKnowledgeBase_IdOrderByIdAsc(Long knowledgeBaseId);

    boolean existsByKnowledgeBase_IdAndUserId(Long knowledgeBaseId, Long userId);
}
