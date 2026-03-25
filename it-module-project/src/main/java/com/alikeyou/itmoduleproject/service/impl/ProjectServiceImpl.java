/**
 * 项目服务实现类
 * 实现ProjectService接口，处理项目相关的业务逻辑
 */
package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectVisibilityEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目服务实现类
 * 实现ProjectService接口，处理项目相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    /**
     * 项目仓库
     */
    private final ProjectRepository projectRepository;
    /**
     * 项目成员仓库
     */
    private final ProjectMemberRepository projectMemberRepository;
    /**
     * 项目权限服务
     */
    private final ProjectPermissionService projectPermissionService;
    /**
     * 项目成员服务
     */
    private final ProjectMemberService projectMemberService;
    /**
     * 项目任务服务
     */
    private final ProjectTaskService projectTaskService;
    /**
     * 项目文件服务
     */
    private final ProjectFileService projectFileService;

    @Override
    @Transactional
    public ProjectDetailVO createProject(ProjectCreateRequest request, Long currentUserId) {
        validateCreateOrUpdate(request.getName(), request.getVisibility(), request.getStatus());
        Project saved = projectRepository.save(Project.builder()
            .name(request.getName().trim())
            .description(request.getDescription())
            .category(request.getCategory())
            .authorId(currentUserId)
            .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : ProjectStatusEnum.DRAFT.getValue())
            .tags(request.getTags())
            .templateId(request.getTemplateId())
            .visibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : ProjectVisibilityEnum.PUBLIC.getValue())
            .build());
        projectMemberRepository.save(ProjectMember.builder()
            .projectId(saved.getId())
            .userId(currentUserId)
            .role(ProjectMemberRoleEnum.OWNER.getValue())
            .status(ProjectMemberStatusEnum.ACTIVE.getValue())
            .build());
        return getProjectDetail(saved.getId(), currentUserId);
    }

    @Override
    @Transactional
    public ProjectDetailVO updateProject(Long projectId, ProjectUpdateRequest request, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException("项目不存在"));
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (request.getName() != null) {
            project.setName(requireText(request.getName(), "项目名称不能为空"));
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            project.setCategory(request.getCategory());
        }
        if (request.getVisibility() != null) {
            if (!ProjectVisibilityEnum.contains(request.getVisibility())) {
                throw new BusinessException("项目可见性不合法");
            }
            project.setVisibility(request.getVisibility());
        }
        if (request.getStatus() != null) {
            if (!ProjectStatusEnum.contains(request.getStatus())) {
                throw new BusinessException("项目状态不合法");
            }
            project.setStatus(request.getStatus());
        }
        if (request.getTags() != null) {
            project.setTags(request.getTags());
        }
        if (request.getTemplateId() != null) {
            project.setTemplateId(request.getTemplateId());
        }
        projectRepository.save(project);
        return getProjectDetail(projectId, currentUserId);
    }

    @Override
    public ProjectDetailVO getProjectDetail(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException("项目不存在"));
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        List<ProjectMemberVO> members = projectMemberService.listMembers(projectId, currentUserId);
        List<ProjectTaskVO> tasks = projectTaskService.listTasks(projectId, currentUserId);
        List<ProjectFileVO> files = projectFileService.listFiles(projectId, currentUserId);
        return ProjectVoMapper.toProjectDetailVO(project, members, tasks, files);
    }

    @Override
    public PageResult<ProjectListVO> pageProjects(String keyword, String status, Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Specification<Project> specification = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.or(
                    cb.like(root.get("name"), "%" + keyword.trim() + "%"),
                    cb.like(root.get("description"), "%" + keyword.trim() + "%")
                ));
            }
            if (StringUtils.hasText(status)) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (authorId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("authorId"), authorId));
            }
            return predicates;
        };
        Page<Project> result = projectRepository.findAll(specification, pageable);
        return new PageResult<>(
            result.getContent().stream().map(ProjectVoMapper::toProjectListVO).toList(),
            result.getTotalElements(),
            page,
            size
        );
    }

    @Override
    public PageResult<ProjectListVO> pageMyProjects(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Page<Project> result = projectRepository.findMyProjects(currentUserId, pageable);
        return new PageResult<>(
            result.getContent().stream().map(ProjectVoMapper::toProjectListVO).toList(),
            result.getTotalElements(),
            page,
            size
        );
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectOwner(projectId, currentUserId);
        projectRepository.deleteById(projectId);
    }

    private void validateCreateOrUpdate(String name, String visibility, String status) {
        requireText(name, "项目名称不能为空");
        if (StringUtils.hasText(visibility) && !ProjectVisibilityEnum.contains(visibility)) {
            throw new BusinessException("项目可见性不合法");
        }
        if (StringUtils.hasText(status) && !ProjectStatusEnum.contains(status)) {
            throw new BusinessException("项目状态不合法");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}