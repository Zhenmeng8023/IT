package com.alikeyou.itmodulecircle.service;

import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.dto.CircleResponse;

import java.util.List;
import java.util.Optional;

public interface CircleService {

    Circle createCircle(Circle circle);

    Circle updateCircle(Long id, Circle circle);

    void deleteCircle(Long id);

    Optional<Circle> getCircleById(Long id);

    List<Circle> getAllCircles();

    List<Circle> getCirclesByCreatorId(Long creatorId);

    List<Circle> getPublicCircles();

    List<Circle> getCirclesByType(String type);

    Optional<Circle> getCircleByName(String name);

    boolean existsCircleByName(String name);

    CircleResponse convertToResponse(Circle circle);

    List<CircleResponse> convertToResponseList(List<Circle> circles);
}
