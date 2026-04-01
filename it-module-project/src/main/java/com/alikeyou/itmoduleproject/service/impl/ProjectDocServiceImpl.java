package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectDocCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectDocUpdateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectDoc;
import com.alikeyou.itmoduleproject.entity.ProjectDocVersion;
import com.alikeyou.itmoduleproject.repository.ProjectDocRepository;
import com.alikeyou.itmoduleproject.repository.ProjectDocVersionRepository;
import com.alikeyou.itmoduleproject.service.ProjectDocService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.ProjectDocListItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectDocServiceImpl implements ProjectDocService {

    private final ProjectDocRepository projectDocRepository;
    private final ProjectDocVersionRepository projectDocVersionRepository;
    private final ProjectPermissionService projectPermissionService;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocListItemVO> listDocs(Long projectId, String type, String keyword, Boolean pinned, Boolean mainReadme, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectDocRepository.findByProjectId(projectId)
                .stream()
                .filter(doc -> matchType(doc, type))
                .filter(doc -> matchKeyword(doc, keyword))
                .filter(doc -> matchPinned(doc, pinned))
                .filter(doc -> matchMainReadme(doc, mainReadme))
                .sorted(docComparator())
                .map(this::toListItemVO)
                .toList();
    }

    @Override
    @Transactional
    public ProjectDocVO createDoc(Long projectId, ProjectDocCreateRequest request, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        validateCreateRequest(request);

        ProjectDoc doc = new ProjectDoc();
        doc.setProjectId(projectId);
        fillDocFromCreateRequest(doc, request);
        doc.setCreatedBy(currentUserId);
        doc.setUpdatedBy(currentUserId);
        doc.setLatestVersionNo(1);

        if (Boolean.TRUE.equals(doc.getIsMainReadme())) {
            clearMainReadme(projectId, null);
        }

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved, 1, currentUserId, normalizeChangeSummary(request.getChangeSummary(), "创建文档"), "save"));
        return toDocVO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVO getDoc(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        return toDocVO(doc);
    }

    @Override
    @Transactional
    public ProjectDocVO updateDoc(Long docId, ProjectDocUpdateRequest request, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        validateUpdateRequest(request);

        fillDocFromUpdateRequest(doc, request);
        doc.setUpdatedBy(currentUserId);
        int nextVersionNo = nextVersionNo(doc.getLatestVersionNo());
        doc.setLatestVersionNo(nextVersionNo);

        if (Boolean.TRUE.equals(doc.getIsMainReadme())) {
            clearMainReadme(doc.getProjectId(), doc.getId());
        }

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved, nextVersionNo, currentUserId, normalizeChangeSummary(request.getChangeSummary(), "更新文档"), "save"));
        return toDocVO(saved);
    }

    @Override
    @Transactional
    public void deleteDoc(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        projectDocVersionRepository.deleteByDocId(docId);
        projectDocRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocVersionVO> listVersions(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        return projectDocVersionRepository.findByDocIdOrderByVersionNoDescCreatedAtDesc(docId)
                .stream()
                .map(this::toVersionVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVersionVO getVersion(Long docId, Integer versionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        ProjectDocVersion version = getVersionEntity(docId, versionNo);
        return toVersionVO(version);
    }

    @Override
    @Transactional
    public ProjectDocVO rollback(Long docId, Integer versionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        ProjectDocVersion version = getVersionEntity(docId, versionNo);

        doc.setTitle(version.getTitleSnapshot());
        doc.setDocType(normalizeDocType(version.getDocTypeSnapshot()));
        doc.setContent(Objects.toString(version.getContentSnapshot(), ""));
        doc.setContentFormat(normalizeContentFormat(version.getContentFormatSnapshot()));
        doc.setSummary(trimToNull(version.getSummarySnapshot()));
        doc.setIsMainReadme(Boolean.TRUE.equals(version.getIsMainReadmeSnapshot()));
        doc.setIsPinnedHome(Boolean.TRUE.equals(version.getIsPinnedHomeSnapshot()));
        doc.setSortNo(version.getSortNoSnapshot() == null ? 0 : version.getSortNoSnapshot());
        doc.setStatus(normalizeStatus(version.getStatusSnapshot()));
        doc.setUpdatedBy(currentUserId);
        int nextVersionNo = nextVersionNo(doc.getLatestVersionNo());
        doc.setLatestVersionNo(nextVersionNo);

        if (Boolean.TRUE.equals(doc.getIsMainReadme())) {
            clearMainReadme(doc.getProjectId(), doc.getId());
        }

        ProjectDoc saved = projectDocRepository.save(doc);
        String changeSummary = "回滚到版本 " + versionNo;
        projectDocVersionRepository.save(buildVersion(saved, nextVersionNo, currentUserId, changeSummary, "rollback"));
        return toDocVO(saved);
    }

    private ProjectDoc getDocEntity(Long docId) {
        return projectDocRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("项目文档不存在"));
    }

    private ProjectDocVersion getVersionEntity(Long docId, Integer versionNo) {
        return projectDocVersionRepository.findByDocIdAndVersionNo(docId, versionNo)
                .orElseThrow(() -> new BusinessException("文档版本不存在"));
    }

    private void validateCreateRequest(ProjectDocCreateRequest request) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("文档标题不能为空");
        }
        if (request.getContent() == null) {
            throw new BusinessException("文档内容不能为空");
        }
    }

    private void validateUpdateRequest(ProjectDocUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("文档标题不能为空");
        }
        if (request.getContent() == null) {
            throw new BusinessException("文档内容不能为空");
        }
    }

    private void fillDocFromCreateRequest(ProjectDoc doc, ProjectDocCreateRequest request) {
        doc.setTitle(request.getTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setContent(request.getContent());
        doc.setContentFormat(normalizeContentFormat(request.getContentFormat()));
        doc.setSummary(trimToNull(request.getSummary()));
        doc.setIsMainReadme(Boolean.TRUE.equals(request.getIsMainReadme()));
        doc.setIsPinnedHome(Boolean.TRUE.equals(request.getIsPinnedHome()));
        doc.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        doc.setStatus(normalizeStatus(request.getStatus()));
    }

    private void fillDocFromUpdateRequest(ProjectDoc doc, ProjectDocUpdateRequest request) {
        doc.setTitle(request.getTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setContent(request.getContent());
        doc.setContentFormat(normalizeContentFormat(request.getContentFormat()));
        doc.setSummary(trimToNull(request.getSummary()));
        doc.setIsMainReadme(Boolean.TRUE.equals(request.getIsMainReadme()));
        doc.setIsPinnedHome(Boolean.TRUE.equals(request.getIsPinnedHome()));
        doc.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        doc.setStatus(normalizeStatus(request.getStatus()));
    }

    private void clearMainReadme(Long projectId, Long excludeId) {
        List<ProjectDoc> mainDocs = projectDocRepository.findByProjectIdAndIsMainReadmeTrueOrderByUpdatedAtDesc(projectId);
        for (ProjectDoc item : mainDocs) {
            if (excludeId != null && excludeId.equals(item.getId())) {
                continue;
            }
            item.setIsMainReadme(false);
            projectDocRepository.save(item);
        }
    }

    private ProjectDocVersion buildVersion(ProjectDoc doc, Integer versionNo, Long currentUserId, String changeSummary, String actionType) {
        return ProjectDocVersion.builder()
                .docId(doc.getId())
                .versionNo(versionNo)
                .titleSnapshot(doc.getTitle())
                .docTypeSnapshot(doc.getDocType())
                .contentSnapshot(doc.getContent())
                .contentFormatSnapshot(doc.getContentFormat())
                .summarySnapshot(doc.getSummary())
                .isMainReadmeSnapshot(doc.getIsMainReadme())
                .isPinnedHomeSnapshot(doc.getIsPinnedHome())
                .sortNoSnapshot(doc.getSortNo())
                .statusSnapshot(doc.getStatus())
                .changeSummary(changeSummary)
                .operatorId(currentUserId)
                .actionType(actionType)
                .build();
    }

    private boolean matchType(ProjectDoc doc, String type) {
        if (!StringUtils.hasText(type)) {
            return true;
        }
        return normalizeDocType(type).equals(normalizeDocType(doc.getDocType()));
    }

    private boolean matchKeyword(ProjectDoc doc, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        String title = Objects.toString(doc.getTitle(), "").toLowerCase(Locale.ROOT);
        String summary = Objects.toString(doc.getSummary(), "").toLowerCase(Locale.ROOT);
        return title.contains(value) || summary.contains(value);
    }

    private boolean matchPinned(ProjectDoc doc, Boolean pinned) {
        if (pinned == null) {
            return true;
        }
        return Objects.equals(Boolean.TRUE.equals(doc.getIsPinnedHome()), pinned);
    }

    private boolean matchMainReadme(ProjectDoc doc, Boolean mainReadme) {
        if (mainReadme == null) {
            return true;
        }
        return Objects.equals(Boolean.TRUE.equals(doc.getIsMainReadme()), mainReadme);
    }

    private Comparator<ProjectDoc> docComparator() {
        return Comparator
                .comparing((ProjectDoc a) -> Boolean.TRUE.equals(a.getIsMainReadme())).reversed()
                .thenComparing(a -> Boolean.TRUE.equals(a.getIsPinnedHome()), Comparator.reverseOrder())
                .thenComparing(a -> a.getSortNo() == null ? 0 : a.getSortNo())
                .thenComparing(ProjectDoc::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectDoc::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Integer nextVersionNo(Integer latestVersionNo) {
        return latestVersionNo == null || latestVersionNo < 1 ? 1 : latestVersionNo + 1;
    }

    private String normalizeDocType(String value) {
        String type = trimToNull(value);
        if (type == null) {
            return "readme";
        }
        String normalized = type.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "readme", "design", "meeting", "manual", "other" -> normalized;
            default -> "other";
        };
    }

    private String normalizeContentFormat(String value) {
        String format = trimToNull(value);
        if (format == null) {
            return "markdown";
        }
        String normalized = format.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "markdown", "md", "html", "text", "plain" -> normalized;
            default -> "markdown";
        };
    }

    private String normalizeStatus(String value) {
        String status = trimToNull(value);
        if (status == null) {
            return "published";
        }
        String normalized = status.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "draft", "published", "archived" -> normalized;
            default -> "published";
        };
    }

    private String normalizeChangeSummary(String value, String fallback) {
        String summary = trimToNull(value);
        return summary == null ? fallback : summary;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectDocVO toDocVO(ProjectDoc doc) {
        return ProjectDocVO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .content(doc.getContent())
                .contentFormat(doc.getContentFormat())
                .summary(doc.getSummary())
                .isMainReadme(doc.getIsMainReadme())
                .isPinnedHome(doc.getIsPinnedHome())
                .sortNo(doc.getSortNo())
                .status(doc.getStatus())
                .latestVersionNo(doc.getLatestVersionNo())
                .createdBy(doc.getCreatedBy())
                .updatedBy(doc.getUpdatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private ProjectDocListItemVO toListItemVO(ProjectDoc doc) {
        return ProjectDocListItemVO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .contentFormat(doc.getContentFormat())
                .summary(doc.getSummary())
                .isMainReadme(doc.getIsMainReadme())
                .isPinnedHome(doc.getIsPinnedHome())
                .sortNo(doc.getSortNo())
                .status(doc.getStatus())
                .latestVersionNo(doc.getLatestVersionNo())
                .createdBy(doc.getCreatedBy())
                .updatedBy(doc.getUpdatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private ProjectDocVersionVO toVersionVO(ProjectDocVersion version) {
        return ProjectDocVersionVO.builder()
                .id(version.getId())
                .docId(version.getDocId())
                .versionNo(version.getVersionNo())
                .titleSnapshot(version.getTitleSnapshot())
                .docTypeSnapshot(version.getDocTypeSnapshot())
                .contentSnapshot(version.getContentSnapshot())
                .contentFormatSnapshot(version.getContentFormatSnapshot())
                .summarySnapshot(version.getSummarySnapshot())
                .isMainReadmeSnapshot(version.getIsMainReadmeSnapshot())
                .isPinnedHomeSnapshot(version.getIsPinnedHomeSnapshot())
                .sortNoSnapshot(version.getSortNoSnapshot())
                .statusSnapshot(version.getStatusSnapshot())
                .changeSummary(version.getChangeSummary())
                .operatorId(version.getOperatorId())
                .actionType(version.getActionType())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
