package com.alikeyou.itmodulecircle.repository;

import com.alikeyou.itmodulecircle.entity.Circle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircleRepository extends JpaRepository<Circle, Long> {

    List<Circle> findByCreatorId(Long creatorId);

    List<Circle> findByVisibility(String visibility);

    List<Circle> findByType(String type);

    Optional<Circle> findByName(String name);

    boolean existsByName(String name);

    List<Circle> findAllByOrderByCreatedAtDesc();

    /**
     * 统计圈子数量
     */
    long count();


}

