package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleCloseRequest;
import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.dto.CircleStatistics;
import com.alikeyou.itmodulecircle.dto.CircleUpdateRequest;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.dto.CircleCreatorInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulecircle.repository.CircleMemberRepository;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CircleServiceImpl implements CircleService {


    private static final Set<String> ALLOWED_TYPES =
            Set.of("pending", "approved", "close", "rejected");

    @Autowired
    private CircleRepository circleRepository;

    @Autowired
    private CircleMemberRepository circleMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CircleCommentRepository circleCommentRepository;

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
            throw new CircleException("圈子类型必须是：pending, approved, close, rejected");
        }

        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());

        // 创建时默认设置为 pending 状态，等待审核
        circle.setType("pending");

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

        // 添加统计信息
        long memberCount = circleMemberRepository.countByCircle(circle);
        long activeMemberCount = circleMemberRepository.countByCircleAndStatus(circle, "active");
        long postCount = circleCommentRepository.countByCircleIdAndParentCommentIdIsNull(circle.getId());

        response.setMemberCount(memberCount);
        response.setActiveMemberCount(activeMemberCount);
        response.setPostCount(postCount);

        return response;
    }

    @Override
    public List<CircleResponse> convertToResponseList(List<Circle> circles) {
        return circles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CircleStatistics getCircleStatistics() {
        // 统计圈子总数
        long totalCircles = circleRepository.count();

        // 统计所有成员关系数 (人员在圈子中的总数)
        long totalMembers = circleMemberRepository.count();

        // 统计活跃成员数 (状态为 'active' 的成员)
        long activeMembers = circleMemberRepository.countByStatus("active");

        // 统计主题帖总数 (parentCommentId 为 NULL 的记录)
        long totalPosts = circleCommentRepository.countByParentCommentIdIsNull();

        return new CircleStatistics(totalCircles, totalMembers, activeMembers, totalPosts);
    }

    @Override
    public long countPostsByCircleId(Long circleId) {
        return circleCommentRepository.countByCircleIdAndParentCommentIdIsNull(circleId);
    }

    @Override
    public Map<String, Long> getCircleStatisticsById(Long circleId) {
        Optional<Circle> circleOpt = circleRepository.findById(circleId);
        if (circleOpt.isEmpty()) {
            return null;
        }

        Circle circle = circleOpt.get();

        // 统计该圈子的成员总数
        long memberCount = circleMemberRepository.countByCircle(circle);

        // 统计该圈子的活跃成员数
        long activeMemberCount = circleMemberRepository.countByCircleAndStatus(circle, "active");

        // 统计该圈子的主题帖数量
        long postCount = circleCommentRepository.countByCircleIdAndParentCommentIdIsNull(circleId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("memberCount", memberCount);
        stats.put("activeMemberCount", activeMemberCount);
        stats.put("postCount", postCount);

        return stats;
    }

    @Override
    @Transactional
    public void approveCircle(Long id) {
        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        if (!"pending".equals(circle.getType())) {
            throw new CircleException("只有待审核的圈子才能通过审核");
        }

        circle.setType("approved");
        circle.setUpdatedAt(Instant.now());
        circleRepository.save(circle);
    }

    @Override
    @Transactional
    public void rejectCircle(Long id) {
        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        if (!"pending".equals(circle.getType())) {
            throw new CircleException("只有待审核的圈子才能拒绝");
        }

        circle.setType("rejected");
        circle.setUpdatedAt(Instant.now());
        circleRepository.save(circle);
    }

    @Override
    @Transactional
    public void batchApproveCircles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Long id : ids) {
            try {
                approveCircle(id);
            } catch (CircleException e) {
                // 记录但不抛出异常，继续处理其他 ID
                System.err.println("审核圈子 " + id + " 失败：" + e.getMessage());
            } catch (Exception e) {
                System.err.println("审核圈子 " + id + " 异常：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    @Transactional
    public void batchRejectCircles(List<Long> ids) {
        for (Long id : ids) {
            try {
                rejectCircle(id);
            } catch (Exception e) {
                // 继续处理其他 ID
            }
        }
    }

    @Override
    public List<Circle> getPendingCircles() {
        return circleRepository.findByTypeOrderByCreatedAtDesc("pending");
    }

    @Override
    public List<Circle> getApprovedPublicCircles() {
        return circleRepository.findByTypeAndVisibilityOrderByCreatedAtDesc("approved", "public");
    }


    @Override
    @Transactional
    public Circle createCircleWithOperator(CircleCreateRequest request) {
        if (request.getCreatorId() == null) {
            throw new CircleException("创建者 ID 不能为空");
        }

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new CircleException("圈子名称不能为空");
        }
        if (existsCircleByName(name)) {
            throw new CircleException("圈子名称已存在");
        }

        UserInfo creator = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + request.getCreatorId()));

        Circle circle = new Circle();
        circle.setName(name);
        circle.setDescription(request.getDescription());
        String visibility = request.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            visibility = "public";
        }
        circle.setVisibility(visibility);

        Integer maxMembers = request.getMaxMembers();
        circle.setMaxMembers(maxMembers != null ? maxMembers : 500);
        circle.setCreatorId(request.getCreatorId());
        circle.setType("pending");
        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());

        Circle savedCircle = saveNewCircle(circle);

        // 自动将创建者添加为圈主
        CircleMember membership = new CircleMember();
        membership.setCircle(savedCircle);
        membership.setUser(creator);
        membership.setJoinTime(Instant.now());
        membership.setStatus("active");
        membership.setRole("owner");
        circleMemberRepository.save(membership);

        return savedCircle;
    }

    private Circle saveNewCircle(Circle circle) {
        try {
            return circleRepository.save(circle);
        } catch (DataIntegrityViolationException e) {
            if (isNameConstraintViolation(e)) {
                throw new CircleException("圈子名称已存在");
            }

            // 兼容旧库：circle.type 可能仍是 enum(official/private/public)
            if (isTypeColumnViolation(e) && "pending".equals(circle.getType())) {
                circle.setType("public");
                return circleRepository.save(circle);
            }
            throw e;
        }
    }

    private boolean isNameConstraintViolation(Throwable throwable) {
        String message = extractMessage(throwable);
        return message.contains("duplicate")
                || message.contains("for key 'name'")
                || message.contains("for key `name`");
    }

    private boolean isTypeColumnViolation(Throwable throwable) {
        String message = extractMessage(throwable);
        return (message.contains("column 'type'") || message.contains("column `type`"))
                && (message.contains("truncated") || message.contains("enum"));
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? "" : message.toLowerCase();
    }
    @Override
    @Transactional
    public Circle updateCircleWithOperator(Long id, CircleUpdateRequest request) {
        Circle existingCircle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            if (!existingCircle.getName().equals(request.getName()) && existsCircleByName(request.getName())) {
                throw new CircleException("圈子名称已存在");
            }
            existingCircle.setName(request.getName());
        }

        if (request.getDescription() != null) {
            existingCircle.setDescription(request.getDescription());
        }

        if (request.getVisibility() != null) {
            existingCircle.setVisibility(request.getVisibility());
        }

        if (request.getMaxMembers() != null) {
            existingCircle.setMaxMembers(request.getMaxMembers());
        }

        existingCircle.setUpdatedAt(Instant.now());

        return circleRepository.save(existingCircle);
    }

    @Override
    @Transactional
    public void closeCircleWithDetail(Long id, CircleCloseRequest request) {
        if (request.getOperatorId() == null) {
            throw new CircleException("操作人 ID 不能为空");
        }

        UserInfo operator = userRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new CircleException("操作人不存在，ID: " + request.getOperatorId()));

        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + id));

        if (!"approved".equals(circle.getType())) {
            throw new CircleException("只有已审核通过的圈子才能关闭");
        }

        circle.setType("close");
        circle.setUpdatedAt(Instant.now());

        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            String closeReason = "\n[关闭原因：" + request.getReason() + "]";
            String currentDesc = circle.getDescription() != null ? circle.getDescription() : "";
            if (!currentDesc.endsWith(closeReason)) {
                circle.setDescription(currentDesc + closeReason);
            }
        }

        circleRepository.save(circle);
    }


}

