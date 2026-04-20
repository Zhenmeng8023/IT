package com.alikeyou.itmodulecircle.service;

import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.entity.CircleMember;

import java.util.List;
import java.util.Optional;

public interface CircleMemberService {

    /**
     * 获取圈子成员列表
     */
    List<CircleMember> getMembersByCircleId(Long circleId);

    /**
     * 获取管理员列表
     */
    List<CircleMember> getAdminsByCircleId(Long circleId);

    /**
     * 根据成员关系 ID 获取成员信息
     */
    Optional<CircleMember> getMemberById(Long memberId);

    /**
     * 加入圈子
     */
    CircleMember joinCircle(Long circleId, Long userId);

    /**
     * 退出圈子
     */
    void leaveCircle(Long circleId, Long userId);

    /**
     * 设置管理员
     */
    CircleMember setAdminRole(Long circleId, Long userId, String role);

    CircleMember setAdminRole(Long circleId, Long userId, String role, Long operatorId);

    /**
     * 根据成员关系 ID 设置角色
     */
    CircleMember setMemberRoleByMemberId(Long memberId, String role);

    CircleMember setMemberRoleByMemberId(Long memberId, String role, Long operatorId);

    /**
     * 移除成员
     */
    void removeMember(Long circleId, Long userId);

    void removeMember(Long circleId, Long userId, Long operatorId);

    /**
     * 根据成员关系 ID 移除成员
     */
    void removeMemberByMemberId(Long memberId);

    void removeMemberByMemberId(Long memberId, Long operatorId);

    /**
     * 检查用户是否是成员
     */
    boolean isMember(Long circleId, Long userId);

    /**
     * 转换为响应对象
     */
    CircleMemberResponse convertToResponse(CircleMember member);

    /**
     * 批量转换
     */
    List<CircleMemberResponse> convertToResponseList(List<CircleMember> members);
}
