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
import com.alikeyou.itmodulecircle.support.CircleCommentVisibilitySupport;
import com.alikeyou.itmodulecircle.support.CircleLifecycleCompat;
import com.alikeyou.itmodulecircle.support.CircleMessageNormalizer;
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

    private static final Set<String> ALLOWED_TYPES = Set.of(
            CircleLifecycleCompat.PENDING,
            CircleLifecycleCompat.APPROVED,
            CircleLifecycleCompat.CLOSED,
            CircleLifecycleCompat.REJECTED
    );
    private static final Set<Integer> MANAGE_ROLE_IDS = Set.of(1, 2, 3);
    private static final Set<String> CIRCLE_MANAGE_MEMBER_ROLES = Set.of("owner", "admin");

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
        if (type != null && !type.isBlank() && !ALLOWED_TYPES.contains(type)) {
            throw new CircleException("圈子类型必须是：pending, approved, close, rejected");
        }

        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());

        // 创建时默认设置为 pending 状态，等待审核
        circle.setType(CircleLifecycleCompat.PENDING);
        circle.setDescription(CircleLifecycleCompat.applyLifecycleMarker(circle.getDescription(), CircleLifecycleCompat.PENDING));

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
        deleteCircle(id, null);
    }

    @Override
    @Transactional
    public void deleteCircle(Long id, Long operatorId) {
        Circle circle = requireCircleManagePermission(id, operatorId);
        circleRepository.delete(circle);
    }

    @Override
    public Circle requireCircleManagePermission(Long circleId, Long operatorId) {
        if (operatorId == null) {
            throw new CircleException(CircleMessageNormalizer.LOGIN_REQUIRED);
        }

        Circle circle = getCircleById(circleId)
                .orElseThrow(() -> new CircleException(CircleMessageNormalizer.RESOURCE_NOT_FOUND));

        UserInfo operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new CircleException(CircleMessageNormalizer.LOGIN_REQUIRED));

        if (hasSystemManagePermission(operator)
                || Objects.equals(circle.getCreatorId(), operatorId)
                || isCircleManager(circle, operator)) {
            return circle;
        }

        throw new CircleException(CircleMessageNormalizer.PERMISSION_DENIED);
    }

    @Override
    public Circle requirePublicVisibleCircle(Long circleId) {
        Circle circle = getCircleById(circleId)
                .orElseThrow(() -> new CircleException(CircleMessageNormalizer.RESOURCE_NOT_FOUND));
        if (!isPublicVisibleCircle(circle)) {
            throw new CircleException(CircleMessageNormalizer.RESOURCE_NOT_FOUND);
        }
        return circle;
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
        return circleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isPublicVisibleCircle)
                .toList();
    }

    @Override
    public List<Circle> getCirclesByType(String type) {
        String normalizedType = normalizeLifecycle(type);
        return circleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(circle -> normalizedType.equals(normalizeLifecycle(circle)))
                .toList();
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
        response.setDescription(CircleLifecycleCompat.stripLifecycleMarker(circle.getDescription()));
        response.setType(getLifecycleStatus(circle));
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
        long postCount = countPublicVisiblePostsByCircleIdInternal(circle.getId());

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
        long totalPosts = countManagePosts(circleCommentRepository.findAllByParentCommentIdIsNullOrderByCreatedAtDesc());

        return new CircleStatistics(totalCircles, totalMembers, activeMembers, totalPosts);
    }

    @Override
    public long countPostsByCircleId(Long circleId) {
        return countPublicVisiblePostsByCircleIdInternal(circleId);
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

        List<com.alikeyou.itmodulecircle.entity.CircleComment> rootPosts =
                circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(circleId);
        long postCount = countPublicVisiblePosts(rootPosts);
        long managePostCount = countManagePosts(rootPosts);
        long pendingPostCount = countPostsByStatus(rootPosts, CircleCommentVisibilitySupport.STATUS_PENDING);
        long publishedPostCount = countPostsByStatus(rootPosts, CircleCommentVisibilitySupport.STATUS_PUBLISHED);
        long deletedPostCount = countPostsByStatus(rootPosts, CircleCommentVisibilitySupport.STATUS_DELETED);

        Map<String, Long> stats = new HashMap<>();
        stats.put("memberCount", memberCount);
        stats.put("activeMemberCount", activeMemberCount);
        stats.put("postCount", postCount);
        stats.put("managePostCount", managePostCount);
        stats.put("pendingPostCount", pendingPostCount);
        stats.put("publishedPostCount", publishedPostCount);
        stats.put("deletedPostCount", deletedPostCount);

        return stats;
    }

    @Override
    @Transactional
    public void approveCircle(Long id) {
        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        if (!CircleLifecycleCompat.isPendingCircle(circle)) {
            throw new CircleException("只有待审核的圈子才能通过审核");
        }

        applyLifecycle(circle, CircleLifecycleCompat.APPROVED);
        circle.setUpdatedAt(Instant.now());
        saveLifecycleChange(circle);
    }

    @Override
    @Transactional
    public void rejectCircle(Long id) {
        Circle circle = getCircleById(id)
                .orElseThrow(() -> new CircleException("圈子不存在"));

        if (!CircleLifecycleCompat.isPendingCircle(circle)) {
            throw new CircleException("只有待审核的圈子才能拒绝");
        }

        applyLifecycle(circle, CircleLifecycleCompat.REJECTED);
        circle.setUpdatedAt(Instant.now());
        saveLifecycleChange(circle);
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
        return circleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(CircleLifecycleCompat::isPendingCircle)
                .toList();
    }

    @Override
    public List<Circle> getApprovedPublicCircles() {
        return circleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isPublicVisibleCircle)
                .toList();
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
        circle.setType(CircleLifecycleCompat.PENDING);
        circle.setDescription(CircleLifecycleCompat.applyLifecycleMarker(request.getDescription(), CircleLifecycleCompat.PENDING));
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
            if (isTypeColumnViolation(e) && CircleLifecycleCompat.PENDING.equals(circle.getType())) {
                circle.setType(resolveLegacyType(circle));
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
        Circle existingCircle = requireCircleManagePermission(id, request.getOperatorId());

        if (request.getName() != null && !request.getName().isEmpty()) {
            if (!existingCircle.getName().equals(request.getName()) && existsCircleByName(request.getName())) {
                throw new CircleException("圈子名称已存在");
            }
            existingCircle.setName(request.getName());
        }

        if (request.getDescription() != null) {
            existingCircle.setDescription(CircleLifecycleCompat.applyLifecycleMarker(
                    request.getDescription(),
                    getLifecycleStatus(existingCircle)
            ));
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
        Circle circle = requireCircleManagePermission(id, request.getOperatorId());

        if (!isApprovedCircle(circle)) {
            throw new CircleException("只有已审核通过的圈子才能关闭");
        }

        String description = CircleLifecycleCompat.stripLifecycleMarker(circle.getDescription());
        circle.setUpdatedAt(Instant.now());

        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            String closeReason = "\n[关闭原因：" + request.getReason() + "]";
            String currentDesc = description != null ? description : "";
            if (!currentDesc.endsWith(closeReason)) {
                description = currentDesc + closeReason;
            }
        }

        circle.setDescription(description);
        applyLifecycle(circle, CircleLifecycleCompat.CLOSED);
        saveLifecycleChange(circle);
    }

    @Override
    public String getLifecycleStatus(Circle circle) {
        return normalizeLifecycle(circle);
    }

    @Override
    public boolean isApprovedCircle(Circle circle) {
        return CircleLifecycleCompat.isApprovedCircle(circle);
    }

    @Override
    public boolean isPublicVisibleCircle(Circle circle) {
        return CircleLifecycleCompat.isPublicVisibleCircle(circle);
    }

    private void applyLifecycle(Circle circle, String lifecycle) {
        circle.setDescription(CircleLifecycleCompat.applyLifecycleMarker(circle.getDescription(), lifecycle));
        if (CircleLifecycleCompat.isWorkflowType(circle.getType())) {
            circle.setType(lifecycle);
        }
    }

    private void saveLifecycleChange(Circle circle) {
        try {
            circleRepository.save(circle);
        } catch (DataIntegrityViolationException e) {
            if (!isTypeColumnViolation(e)) {
                throw e;
            }
            circle.setType(resolveLegacyType(circle));
            circleRepository.save(circle);
        }
    }

    private String resolveLegacyType(Circle circle) {
        String visibility = circle.getVisibility() == null
                ? CircleLifecycleCompat.VISIBILITY_PUBLIC
                : circle.getVisibility().trim().toLowerCase();
        return CircleLifecycleCompat.VISIBILITY_PRIVATE.equals(visibility)
                ? CircleLifecycleCompat.VISIBILITY_PRIVATE
                : CircleLifecycleCompat.VISIBILITY_PUBLIC;
    }

    private String normalizeLifecycle(Circle circle) {
        return CircleLifecycleCompat.getLifecycleStatus(circle);
    }

    private String normalizeLifecycle(String type) {
        return CircleLifecycleCompat.getLifecycleStatus(type, null);
    }

    private boolean hasSystemManagePermission(UserInfo operator) {
        return operator != null && operator.getRoleId() != null && MANAGE_ROLE_IDS.contains(operator.getRoleId());
    }

    private boolean isCircleManager(Circle circle, UserInfo operator) {
        return circleMemberRepository.findByCircleAndUser(circle, operator)
                .map(CircleMember::getRole)
                .map(role -> role == null ? null : role.trim().toLowerCase())
                .filter(CIRCLE_MANAGE_MEMBER_ROLES::contains)
                .isPresent();
    }

    private long countPublicVisiblePostsByCircleIdInternal(Long circleId) {
        if (circleId == null) {
            return 0L;
        }
        return countPublicVisiblePosts(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(circleId));
    }

    private long countPublicVisiblePosts(List<com.alikeyou.itmodulecircle.entity.CircleComment> rootPosts) {
        return rootPosts.stream()
                .filter(CircleCommentVisibilitySupport::isPublicVisiblePost)
                .count();
    }

    private long countManagePosts(List<com.alikeyou.itmodulecircle.entity.CircleComment> rootPosts) {
        return rootPosts == null ? 0L : rootPosts.size();
    }

    private long countPostsByStatus(List<com.alikeyou.itmodulecircle.entity.CircleComment> rootPosts, String normalizedStatus) {
        return rootPosts.stream()
                .map(post -> CircleCommentVisibilitySupport.normalizeStatus(post.getStatus()))
                .filter(normalizedStatus::equals)
                .count();
    }
}

