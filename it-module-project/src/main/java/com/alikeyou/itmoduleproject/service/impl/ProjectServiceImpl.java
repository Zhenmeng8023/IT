package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectVisibilityEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectDownloadRecordService;
import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.service.ProjectMilestoneService;
import com.alikeyou.itmoduleproject.service.ProjectReleaseService;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.service.ProjectSprintService;
import com.alikeyou.itmoduleproject.service.ProjectStarService;
import com.alikeyou.itmoduleproject.service.ProjectStatService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectHardDeleteSupport;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectMemberService projectMemberService;
    private final ProjectCodeRepositoryService projectCodeRepositoryService;
    private final ProjectTaskService projectTaskService;
    private final ProjectFileService projectFileService;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectStarService projectStarService;
    private final ProjectMilestoneService projectMilestoneService;
    private final ProjectReleaseService projectReleaseService;
    private final ProjectSprintService projectSprintService;
    private final ProjectDownloadRecordService projectDownloadRecordService;
    private final ProjectStatService projectStatService;
    private final ProjectHardDeleteSupport projectHardDeleteSupport;

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

        projectCodeRepositoryService.initRepository(saved.getId(), currentUserId);
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
        List<ProjectTaskVO> tasks = canReadTaskCollaboration(project, currentUserId)
                ? projectTaskService.listTasks(projectId, currentUserId)
                : Collections.emptyList();
        List<ProjectFileVO> files = projectFileService.listFiles(projectId, currentUserId);

        UserInfoLite author = null;
        if (project.getAuthorId() != null) {
            author = projectUserAssembler
                    .mapByIds(Collections.singletonList(project.getAuthorId()))
                    .get(project.getAuthorId());
        }

        ProjectDetailVO vo = ProjectVoMapper.toProjectDetailVO(project, author, members, tasks, files);
        vo.setStarred(projectStarService.isStarred(projectId, currentUserId));
        vo.setContributors(listProjectContributors(projectId, currentUserId));
        vo.setRelatedProjects(listRelatedProjects(projectId, currentUserId, 6));
        vo.setMilestoneOverview(projectMilestoneService.getOverview(projectId, currentUserId));
        vo.setLatestReleaseSummary(projectReleaseService.getLatestReleaseSummary(projectId, currentUserId));
        vo.setCurrentSprintSummary(projectSprintService.getCurrentSprintSummary(projectId, currentUserId));
        vo.setDownloadSummary(projectDownloadRecordService.getSummary(projectId, currentUserId));
        vo.setStatOverview(projectStatService.getOverview(projectId, currentUserId));
        return vo;
    }

    private boolean canReadTaskCollaboration(Project project, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(project.getAuthorId())) {
            return true;
        }
        return projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .isPresent();
    }

    @Override
    public List<ProjectMemberVO> listProjectContributors(Long projectId, Long currentUserId) {
        projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        List<ProjectMember> members = projectMemberRepository.findByProjectIdAndStatusOrderByJoinedAtAsc(
                projectId,
                ProjectMemberStatusEnum.ACTIVE.getValue()
        );

        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(
                members.stream().map(ProjectMember::getUserId).toList()
        );

        return members.stream()
                .sorted(Comparator
                        .comparingInt((ProjectMember member) -> roleOrder(member.getRole()))
                        .thenComparing(ProjectMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(member -> ProjectVoMapper.toProjectMemberVO(member, userMap.get(member.getUserId())))
                .toList();
    }

    @Override
    public List<ProjectListVO> listRelatedProjects(Long projectId, Long currentUserId, int size) {
        int safeSize = Math.max(1, Math.min(size, 20));
        Project currentProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));

        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        Pageable pageable = PageRequest.of(0, Math.max(safeSize * 4, 12), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Specification<Project> specification = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(buildReadableScopePredicate(root, query, cb, currentUserId));
            predicates.add(cb.notEqual(root.get("id"), projectId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Project> result = projectRepository.findAll(specification, pageable);
        List<Project> candidates = result.getContent();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> currentTags = parseTags(currentProject.getTags());
        Map<Long, UserInfoLite> authorMap = projectUserAssembler.mapByIds(
                candidates.stream().map(Project::getAuthorId).filter(Objects::nonNull).distinct().toList()
        );
        Set<Long> starredProjectIds = projectStarService.findStarredProjectIds(
                currentUserId,
                candidates.stream().map(Project::getId).toList()
        );

        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((Project project) -> similarityScore(currentProject, currentTags, project)).reversed()
                        .thenComparing(Project::getStars, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Project::getDownloads, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Project::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeSize)
                .map(project -> {
                    ProjectListVO vo = ProjectVoMapper.toProjectListVO(project, authorMap.get(project.getAuthorId()));
                    vo.setStarred(starredProjectIds.contains(project.getId()));
                    return vo;
                })
                .toList();
    }

    @Override
    public PageResult<ProjectListVO> pageProjects(String keyword,
                                                  String status,
                                                  Long authorId,
                                                  String visibility,
                                                  String category,
                                                  String tag,
                                                  String sortBy,
                                                  Long currentUserId,
                                                  int page,
                                                  int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sortBy));
        Specification<Project> specification = buildProjectPageSpecification(keyword, status, authorId, visibility, category, tag, currentUserId);
        Page<Project> result = projectRepository.findAll(specification, pageable);
        return new PageResult<>(
                toProjectListVOs(result.getContent(), currentUserId),
                result.getTotalElements(),
                page,
                size
        );
    }

    @Override
    public PageResult<ProjectListVO> pageMyProjects(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> result = projectRepository.findMyProjects(currentUserId, pageable);
        return new PageResult<>(
                toProjectListVOs(result.getContent(), currentUserId),
                result.getTotalElements(),
                page,
                size
        );
    }

    @Override
    public PageResult<ProjectListVO> pageParticipatedProjects(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> result = projectRepository.findParticipatedProjects(currentUserId, pageable);
        return new PageResult<>(
                toProjectListVOs(result.getContent(), currentUserId),
                result.getTotalElements(),
                page,
                size
        );
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectOwner(projectId, currentUserId);
        projectHardDeleteSupport.hardDeleteProject(projectId);
    }

    private List<ProjectListVO> toProjectListVOs(List<Project> projects, Long currentUserId) {
        Map<Long, UserInfoLite> authorMap = projectUserAssembler.mapByIds(
                projects.stream().map(Project::getAuthorId).toList()
        );
        Set<Long> starredProjectIds = projectStarService.findStarredProjectIds(
                currentUserId,
                projects.stream().map(Project::getId).toList()
        );
        return projects.stream()
                .map(project -> {
                    ProjectListVO vo = ProjectVoMapper.toProjectListVO(project, authorMap.get(project.getAuthorId()));
                    vo.setStarred(starredProjectIds.contains(project.getId()));
                    return vo;
                })
                .toList();
    }

    private Specification<Project> buildProjectPageSpecification(String keyword,
                                                                 String status,
                                                                 Long authorId,
                                                                 String visibility,
                                                                 String category,
                                                                 String tag,
                                                                 Long currentUserId) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(buildReadableScopePredicate(root, query, cb, currentUserId));
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("description"), like)
                ));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("authorId"), authorId));
            }
            if (StringUtils.hasText(visibility)) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(tag)) {
                predicates.add(cb.like(root.get("tags"), "%" + tag.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildReadableScopePredicate(jakarta.persistence.criteria.Root<Project> root,
                                                  jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                  jakarta.persistence.criteria.CriteriaBuilder cb,
                                                  Long currentUserId) {
        Predicate isPublic = cb.equal(root.get("visibility"), ProjectVisibilityEnum.PUBLIC.getValue());
        if (currentUserId == null) {
            return isPublic;
        }

        jakarta.persistence.criteria.Subquery<Long> membershipSubquery = query.subquery(Long.class);
        jakarta.persistence.criteria.Root<ProjectMember> memberRoot = membershipSubquery.from(ProjectMember.class);
        membershipSubquery.select(memberRoot.get("projectId"))
                .where(
                        cb.equal(memberRoot.get("projectId"), root.get("id")),
                        cb.equal(memberRoot.get("userId"), currentUserId),
                        cb.equal(memberRoot.get("status"), ProjectMemberStatusEnum.ACTIVE.getValue())
                );

        Predicate isOwner = cb.equal(root.get("authorId"), currentUserId);
        Predicate isMember = cb.exists(membershipSubquery);
        return cb.or(isPublic, isOwner, isMember);
    }

    private Sort resolveSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.trim()) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "stars");
            case "downloads" -> Sort.by(Sort.Direction.DESC, "downloads");
            case "updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private int roleOrder(String role) {
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(role)) {
            return 0;
        }
        if (ProjectMemberRoleEnum.ADMIN.getValue().equals(role)) {
            return 1;
        }
        if (ProjectMemberRoleEnum.MEMBER.getValue().equals(role)) {
            return 2;
        }
        return 3;
    }

    private int similarityScore(Project currentProject, Set<String> currentTags, Project candidate) {
        int score = 0;
        if (sameText(currentProject.getCategory(), candidate.getCategory())) {
            score += 100;
        }
        Set<String> candidateTags = parseTags(candidate.getTags());
        if (!currentTags.isEmpty() && !candidateTags.isEmpty()) {
            Set<String> intersection = new LinkedHashSet<>(currentTags);
            intersection.retainAll(candidateTags);
            score += intersection.size() * 10;
        }
        return score;
    }

    private boolean sameText(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private Set<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptySet();
        }
        String normalized = tags.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\"", "").replace("'", "");
        return Arrays.stream(normalized.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    private String requireText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(message);
        }
        return text.trim();
    }
}
