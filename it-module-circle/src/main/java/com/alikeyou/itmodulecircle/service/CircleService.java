package com.alikeyou.itmodulecircle.service;

import com.alikeyou.itmodulecircle.dto.CircleCloseRequest;
import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.dto.CircleUpdateRequest;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.dto.CircleResponse;

import java.util.List;
import java.util.Optional;

public interface CircleService {

    Circle createCircle(Circle circle);

    Circle updateCircle(Long id, Circle circle);

    void deleteCircle(Long id);

    void deleteCircle(Long id, Long operatorId);

    /**
     * 创建圈子（带操作人）
     */
    Circle createCircleWithOperator(CircleCreateRequest request);

    /**
     * 更新圈子（带操作人）
     */
    Circle updateCircleWithOperator(Long id, CircleUpdateRequest request);

    /**
     * 关闭圈子（带操作人和原因）
     */
    void closeCircleWithDetail(Long id, CircleCloseRequest request);

    Circle requireCircleManagePermission(Long circleId, Long operatorId);

    Circle requirePublicVisibleCircle(Long circleId);

    String getLifecycleStatus(Circle circle);

    boolean isApprovedCircle(Circle circle);

    boolean isPublicVisibleCircle(Circle circle);


    /**
     * 审核通过圈子
     */
    void approveCircle(Long id);

    /**
     * 拒绝圈子
     */
    void rejectCircle(Long id);

    /**
     * 批量审核通过圈子
     */
    void batchApproveCircles(List<Long> ids);

    /**
     * 批量拒绝圈子
     */
    void batchRejectCircles(List<Long> ids);

    Optional<Circle> getCircleById(Long id);

    List<Circle> getAllCircles();

    List<Circle> getCirclesByCreatorId(Long creatorId);

    List<Circle> getPublicCircles();

    List<Circle> getCirclesByType(String type);

    Optional<Circle> getCircleByName(String name);

    boolean existsCircleByName(String name);

    CircleResponse convertToResponse(Circle circle);

    List<CircleResponse> convertToResponseList(List<Circle> circles);

    /**
     * 获取圈子统计数据：圈子总数、人员总数、活跃人数
     */
    com.alikeyou.itmodulecircle.dto.CircleStatistics getCircleStatistics();

    /**
     * 获取单个圈子的统计数据
     */
    java.util.Map<String, Long> getCircleStatisticsById(Long circleId);

    /**
     * 获取圈子的主题帖数量
     */
    long countPostsByCircleId(Long circleId);

    /**
     * 获取待审核的圈子列表
     */
    List<Circle> getPendingCircles();

    /**
     * 获取公开且已审核通过的圈子列表
     */
    List<Circle> getApprovedPublicCircles();

}
