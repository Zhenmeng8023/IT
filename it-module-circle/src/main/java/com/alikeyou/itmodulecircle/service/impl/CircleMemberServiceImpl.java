package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleMemberRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CircleMemberServiceImpl implements CircleMemberService {

    @Autowired
    private CircleMemberRepository circleMemberRepository;

    @Autowired
    private CircleRepository circleRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> ALLOWED_ROLES = List.of("owner", "admin", "moderator", "member");

    @Override
    @Transactional(readOnly = true)
    public List<CircleMember> getMembersByCircleId(Long circleId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        return circleMemberRepository.findByCircleOrderByJoinTimeDesc(circle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleMember> getAdminsByCircleId(Long circleId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        return circleMemberRepository.findByCircleAndRoleOrderByJoinTimeDesc(circle, "admin");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<CircleMember> getMemberById(Long memberId) {
        if (memberId == null) {
            throw new CircleException("成员关系 ID 不能为空");
        }
        return circleMemberRepository.findById(memberId);
    }

    @Override
    @Transactional
    public CircleMember joinCircle(Long circleId, Long userId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        if (userId == null) {
            throw new CircleException("用户 ID 不能为空");
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + userId));

        // 检查是否已是成员
        if (circleMemberRepository.existsByCircleAndUser(circle, user)) {
            throw new CircleException("用户已经是圈子成员");
        }

        // 检查是否超过最大成员数
        long currentMemberCount = circleMemberRepository.countByCircle(circle);
        if (circle.getMaxMembers() != null && currentMemberCount >= circle.getMaxMembers()) {
            throw new CircleException("圈子已达到最大成员数限制");
        }

        CircleMember member = new CircleMember();
        member.setCircle(circle);
        member.setUser(user);
        member.setJoinTime(Instant.now());
        member.setStatus("active");
        member.setRole("member");

        return circleMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void leaveCircle(Long circleId, Long userId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        if (userId == null) {
            throw new CircleException("用户 ID 不能为空");
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + userId));

        CircleMember member = circleMemberRepository.findByCircleAndUser(circle, user)
                .orElseThrow(() -> new CircleException("用户不是圈子成员"));

        // 如果是圈主，不允许退出（需要先转让圈主身份）
        if ("owner".equals(member.getRole())) {
            throw new CircleException("圈主不能退出圈子，请先转让圈主身份");
        }

        circleMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public CircleMember setAdminRole(Long circleId, Long userId, String role) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        if (userId == null) {
            throw new CircleException("用户 ID 不能为空");
        }

        String normalizedRole = normalizeRole(role);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new CircleException("无效的角色类型，只能是：" + ALLOWED_ROLES);
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + userId));

        CircleMember member = circleMemberRepository.findByCircleAndUser(circle, user)
                .orElseThrow(() -> new CircleException("用户不是圈子成员"));

        if ("owner".equals(member.getRole()) && !"owner".equals(normalizedRole)) {
            throw new CircleException("圈主角色不能直接变更，请先完成圈主转让");
        }

        member.setRole(normalizedRole);
        return circleMemberRepository.save(member);
    }

    @Override
    @Transactional
    public CircleMember setMemberRoleByMemberId(Long memberId, String role) {
        if (memberId == null) {
            throw new CircleException("成员关系 ID 不能为空");
        }

        String normalizedRole = normalizeRole(role);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new CircleException("无效的角色类型，只能是：" + ALLOWED_ROLES);
        }

        CircleMember member = circleMemberRepository.findById(memberId)
                .orElseThrow(() -> new CircleException("成员关系不存在，ID: " + memberId));

        if ("owner".equals(member.getRole()) && !"owner".equals(normalizedRole)) {
            throw new CircleException("圈主角色不能直接变更，请先完成圈主转让");
        }

        member.setRole(normalizedRole);
        return circleMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Long circleId, Long userId) {
        if (circleId == null) {
            throw new CircleException("圈子 ID 不能为空");
        }

        if (userId == null) {
            throw new CircleException("用户 ID 不能为空");
        }

        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new CircleException("圈子不存在，ID: " + circleId));

        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new CircleException("用户不存在，ID: " + userId));

        CircleMember member = circleMemberRepository.findByCircleAndUser(circle, user)
                .orElseThrow(() -> new CircleException("用户不是圈子成员"));

        if ("owner".equals(member.getRole())) {
            throw new CircleException("圈主不能被移除，请先转让圈主身份");
        }

        circleMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void removeMemberByMemberId(Long memberId) {
        if (memberId == null) {
            throw new CircleException("成员关系 ID 不能为空");
        }

        CircleMember member = circleMemberRepository.findById(memberId)
                .orElseThrow(() -> new CircleException("成员关系不存在，ID: " + memberId));

        if ("owner".equals(member.getRole())) {
            throw new CircleException("圈主不能被移除，请先转让圈主身份");
        }

        circleMemberRepository.delete(member);
    }

    @Override
    public boolean isMember(Long circleId, Long userId) {
        if (circleId == null || userId == null) {
            return false;
        }

        Circle circle = circleRepository.findById(circleId).orElse(null);
        if (circle == null) {
            return false;
        }

        UserInfo user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        return circleMemberRepository.existsByCircleAndUser(circle, user);
    }

    @Override
    public CircleMemberResponse convertToResponse(CircleMember member) {
        if (member == null) {
            return null;
        }

        CircleMemberResponse response = new CircleMemberResponse();
        response.setId(member.getId());
        response.setCircleId(member.getCircle() != null ? member.getCircle().getId() : null);
        response.setUserId(member.getUser().getId());
        response.setUsername(member.getUser().getUsername());
        response.setNickname(member.getUser().getNickname());
        response.setAvatarUrl(member.getUser().getAvatarUrl());
        response.setAvatar(member.getUser().getAvatarUrl());
        response.setJoinTime(member.getJoinTime());
        response.setLastActive(member.getJoinTime());
        response.setStatus(member.getStatus());
        response.setRole(member.getRole());

        return response;
    }

    @Override
    public List<CircleMemberResponse> convertToResponseList(List<CircleMember> members) {
        return members.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toLowerCase();
    }
}
