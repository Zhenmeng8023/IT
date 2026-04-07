package com.alikeyou.itmodulerecommend.repository;

import com.alikeyou.itmodulerecommend.entiey.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    Optional<RecommendationResult> findTopByUser_IdOrderByGeneratedAtDescIdDesc(Long userId);
}
