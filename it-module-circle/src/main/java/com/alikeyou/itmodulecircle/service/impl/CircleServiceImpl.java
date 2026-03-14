package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.dto.CircleCreatorInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alikeyou.itmodulelogin.repository.UserRepository;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CircleServiceImpl implements CircleService {


    private static final Set<String> ALLOWED_TYPES =
            Set.of("official", "private", "public");

    @Autowired
    private CircleRepository circleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Circle createCircle(Circle circle) {
        if (circle.getName() == null || circle.getName().isEmpty()) {
            throw new CircleException("圈子名称不能为空");
        }

        if (existsCircleByName(circle.getName())) {
            throw new CircleException("圈子名称已存在");
        }

        if (circle.getCreatorId() == null) {
            throw new CircleException("创建者 ID 不能为空");
        }

        UserInfo creator = userRepository.findById(circle.getCreatorId())
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + circle.getCreatorId()));

        String type = circle.getType();
        if (type != null && !ALLOWED_TYPES.contains(type)) {
            throw new CircleException("圈子类型必须是：official, private 或 public");
        }

        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());

        if (circle.getType() == null) {
            circle.setType("public");
        }

        if (circle.getVisibility() == null) {
            circle.setVisibility("public");
        }

        if (circle.getMaxMembers() == null) {
            circle.setMaxMembers(500);
        }

        return circleRepository.save(circle);
    }

    @Override
    @Transactional
    public Circle updateCircle(Long id, Circle circle) {
        Circle existingCircle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        if (circle.getName() != null && !circle.getName().isEmpty()) {
            if (!existingCircle.getName().equals(circle.getName()) && existsCircleByName(circle.getName())) {
                throw new CircleException("圈子名称已存在");
            }
            existingCircle.setName(circle.getName());
        }

        if (circle.getDescription() != null) {
            existingCircle.setDescription(circle.getDescription());
        }

        if (circle.getType() != null) {
            if (!ALLOWED_TYPES.contains(circle.getType())) {
                throw new CircleException("圈子类型必须是: official, private 或 public");
            }
            existingCircle.setType(circle.getType());
        }

        if (circle.getVisibility() != null) {
            existingCircle.setVisibility(circle.getVisibility());
        }

        if (circle.getMaxMembers() != null) {
            existingCircle.setMaxMembers(circle.getMaxMembers());
        }

        existingCircle.setUpdatedAt(Instant.now());

        return circleRepository.save(existingCircle);
    }

    @Override
    @Transactional
    public void deleteCircle(Long id) {
        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        circleRepository.delete(circle);
    }

    @Override
    public Optional<Circle> getCircleById(Long id) {
        return circleRepository.findById(id);
    }

    @Override
    public List<Circle> getAllCircles() {
        return circleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Circle> getCirclesByCreatorId(Long creatorId) {
        return circleRepository.findByCreatorId(creatorId);
    }

    @Override
    public List<Circle> getPublicCircles() {
        return circleRepository.findByVisibility("public");
    }

    @Override
    public List<Circle> getCirclesByType(String type) {
        return circleRepository.findByType(type);
    }

    @Override
    public Optional<Circle> getCircleByName(String name) {
        return circleRepository.findByName(name);
    }

    @Override
    public boolean existsCircleByName(String name) {
        return circleRepository.existsByName(name);
    }

    @Override
    public CircleResponse convertToResponse(Circle circle) {
        if (circle == null) {
            return null;
        }

        CircleResponse response = new CircleResponse();
        response.setId(circle.getId());
        response.setName(circle.getName());
        response.setDescription(circle.getDescription());
        response.setType(circle.getType());
        response.setVisibility(circle.getVisibility());
        response.setMaxMembers(circle.getMaxMembers());
        response.setCreatedAt(circle.getCreatedAt());
        response.setUpdatedAt(circle.getUpdatedAt());

        if (circle.getCreatorId() != null) {
            UserInfo creator = userRepository.findById(circle.getCreatorId()).orElse(null);
            if (creator != null) {
                CircleCreatorInfo creatorInfo = new CircleCreatorInfo(
                        creator.getId(),
                        creator.getUsername() != null ? creator.getUsername() : "未知用户",
                        creator.getAvatarUrl()
                );
                response.setCreator(creatorInfo);
            }
        }

        return response;
    }

    @Override
    public List<CircleResponse> convertToResponseList(List<Circle> circles) {
        return circles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}
