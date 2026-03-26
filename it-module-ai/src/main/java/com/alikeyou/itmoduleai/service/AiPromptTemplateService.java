package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiPromptTemplateService {

    Page<AiPromptTemplate> page(Pageable pageable);

    List<AiPromptTemplate> listByScene(String sceneCode);

    List<AiPromptTemplate> listByOwner(Long ownerId);

    AiPromptTemplate getById(Long id);

    AiPromptTemplate save(AiPromptTemplate entity);

    AiPromptTemplate update(Long id, AiPromptTemplate entity);

    AiPromptTemplate publish(Long id);

    AiPromptTemplate disable(Long id);
}
