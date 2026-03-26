package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import com.alikeyou.itmoduleai.service.AiModelService;
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
public class AiModelServiceImpl implements AiModelService {

    private final AiModelRepository aiModelRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AiModel> page(Pageable pageable) {
        return aiModelRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getById(Long id) {
        return aiModelRepository.findById(id).orElseThrow(() -> new RuntimeException("AI 模型不存在"));
    }

    @Override
    public AiModel save(AiModel entity) {
        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return aiModelRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> listEnabled() {
        return aiModelRepository.findByIsEnabledTrueOrderByPriorityAscIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getActiveModel() {
        return aiModelRepository.findFirstByIsEnabledTrueOrderByPriorityAscIdAsc()
                .orElseThrow(() -> new RuntimeException("没有启用的 AI 模型"));
    }

    @Override
    public AiModel enable(Long id) {
        List<AiModel> enabled = aiModelRepository.findByIsEnabledTrueOrderByPriorityAscIdAsc();
        Instant now = Instant.now();
        for (AiModel item : enabled) {
            item.setIsEnabled(false);
            item.setUpdatedAt(now);
        }
        aiModelRepository.saveAll(enabled);
        AiModel current = getById(id);
        current.setIsEnabled(true);
        current.setUpdatedAt(now);
        return aiModelRepository.save(current);
    }

    @Override
    public void disable(Long id) {
        AiModel entity = getById(id);
        entity.setIsEnabled(false);
        entity.setUpdatedAt(Instant.now());
        aiModelRepository.save(entity);
    }
}
