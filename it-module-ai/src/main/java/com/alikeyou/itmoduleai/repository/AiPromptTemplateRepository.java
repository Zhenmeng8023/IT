package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {

    List<AiPromptTemplate> findBySceneCodeAndIsEnabledTrueOrderByVersionNoDesc(String sceneCode);

    List<AiPromptTemplate> findByScopeTypeAndIsEnabledTrueOrderByUpdatedAtDesc(AiPromptTemplate.ScopeType scopeType);

    List<AiPromptTemplate> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
