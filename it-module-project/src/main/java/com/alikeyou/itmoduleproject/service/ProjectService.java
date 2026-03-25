/**
 * 项目服务接口
 * 定义项目相关的业务逻辑方法
 */
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;

/**
 * 项目服务接口
 * 定义项目相关的业务逻辑方法
 */
public interface ProjectService {
    /**
     * 创建项目
     * @param request 创建项目请求参数
     * @param userId 用户ID
     * @return 项目详情
     */
    ProjectDetailVO createProject(ProjectCreateRequest request, Long userId);
    
    /**
     * 更新项目
     * @param id 项目ID
     * @param request 更新项目请求参数
     * @param userId 用户ID
     * @return 项目详情
     */
    ProjectDetailVO updateProject(Long id, ProjectUpdateRequest request, Long userId);
    
    /**
     * 获取项目详情
     * @param id 项目ID
     * @param userId 用户ID
     * @return 项目详情
     */
    ProjectDetailVO getProjectDetail(Long id, Long userId);
    
    /**
     * 分页查询项目列表
     * @param keyword 搜索关键词
     * @param status 项目状态
     * @param authorId 作者ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 项目分页列表
     */
    PageResult<ProjectListVO> pageProjects(String keyword, String status, Long authorId, int page, int size);
    
    /**
     * 分页查询我的项目列表
     * @param userId 用户ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 项目分页列表
     */
    PageResult<ProjectListVO> pageMyProjects(Long userId, int page, int size);
    
    /**
     * 删除项目
     * @param id 项目ID
     * @param userId 用户ID
     */
    void deleteProject(Long id, Long userId);
}