package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import com.alikeyou.itmoduleai.service.AiPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService {

    private final AiPromptTemplateRepository aiPromptTemplateRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AiPromptTemplate> page(Pageable pageable) {
        return aiPromptTemplateRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPromptTemplate> listByScene(String sceneCode) {
        return aiPromptTemplateRepository.findBySceneCodeAndIsEnabledTrueOrderByVersionNoDesc(sceneCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPromptTemplate> listByOwner(Long ownerId) {
        return aiPromptTemplateRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiPromptTemplate getById(Long id) {
        return aiPromptTemplateRepository.findById(id).orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));
    }

    @Override
    public AiPromptTemplate save(AiPromptTemplate entity) {
        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getIsEnabled() == null) {
            entity.setIsEnabled(true);
        }
        if (entity.getPublishStatus() == null) {
            entity.setPublishStatus(AiPromptTemplate.PublishStatus.DRAFT);
        }
        entity.setUpdatedAt(now);
        return aiPromptTemplateRepository.save(entity);
    }

    @Override
    public AiPromptTemplate update(Long id, AiPromptTemplate entity) {
        AiPromptTemplate current = getById(id);
        current.setSceneCode(entity.getSceneCode());
        current.setTemplateName(entity.getTemplateName());
        current.setTemplateType(entity.getTemplateType());
        current.setScopeType(entity.getScopeType());
        current.setProjectId(entity.getProjectId());
        current.setOwnerId(entity.getOwnerId());
        current.setDefaultModel(entity.getDefaultModel());
        current.setSystemPrompt(entity.getSystemPrompt());
        current.setUserPromptTemplate(entity.getUserPromptTemplate());
        current.setVariablesSchema(entity.getVariablesSchema());
        current.setOutputSchema(entity.getOutputSchema());
        current.setVersionNo(entity.getVersionNo());
        current.setRemark(entity.getRemark());
        current.setIsEnabled(entity.getIsEnabled());
        current.setUpdatedAt(Instant.now());
        return aiPromptTemplateRepository.save(current);
    }

    @Override
    public AiPromptTemplate publish(Long id) {
        AiPromptTemplate entity = getById(id);
        entity.setPublishStatus(AiPromptTemplate.PublishStatus.PUBLISHED);
        entity.setIsEnabled(true);
        entity.setUpdatedAt(Instant.now());
        return aiPromptTemplateRepository.save(entity);
    }

    @Override
    public AiPromptTemplate disable(Long id) {
        AiPromptTemplate entity = getById(id);
        entity.setPublishStatus(AiPromptTemplate.PublishStatus.DISABLED);
        entity.setIsEnabled(false);
        entity.setUpdatedAt(Instant.now());
        return aiPromptTemplateRepository.save(entity);
    }
}
