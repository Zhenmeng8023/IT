package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    Optional<AiModel> findByModelName(String modelName);

    List<AiModel> findByIsEnabledTrue();

    List<AiModel> findByModelType(AiModel.ModelType modelType);
}