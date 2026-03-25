/**
 * 项目成员服务接口
 * 定义项目成员相关的业务逻辑方法
 */
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;

import java.util.List;

/**
 * 项目成员服务接口
 * 定义项目成员相关的业务逻辑方法
 */
public interface ProjectMemberService {
    /**
     * 获取项目成员列表
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 成员列表
     */
    List<ProjectMemberVO> listMembers(Long projectId, Long userId);
    
    /**
     * 添加项目成员
     * @param request 添加成员请求参数
     * @param currentUserId 当前用户ID
     * @return 成员信息
     */
    ProjectMemberVO addMember(ProjectMemberAddRequest request, Long currentUserId);
    
    /**
     * 更新成员角色
     * @param request 更新角色请求参数
     * @param currentUserId 当前用户ID
     * @return 成员信息
     */
    ProjectMemberVO updateMemberRole(ProjectMemberRoleUpdateRequest request, Long currentUserId);
    
    /**
     * 移除项目成员
     * @param memberId 成员ID
     * @param currentUserId 当前用户ID
     */
    void removeMember(Long memberId, Long currentUserId);
}