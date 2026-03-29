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
     * 获取待审核的圈子列表
     */
    List<Circle> findByTypeOrderByCreatedAtDesc(String type);


    /**
     * 统计圈子数量
     */
    long count();

    /**
     * 获取公开且已审核通过的圈子列表
     */
    List<Circle> findByTypeAndVisibilityOrderByCreatedAtDesc(String type, String visibility);


}

