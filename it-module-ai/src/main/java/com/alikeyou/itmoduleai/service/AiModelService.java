package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.entity.AiModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiModelService {

    Page<AiModel> page(Pageable pageable);

    AiModel getById(Long id);

    AiModel save(AiModel entity);

    List<AiModel> listEnabled();

    AiModel getActiveModel();

    AiModel enable(Long id);

    void disable(Long id);
}
