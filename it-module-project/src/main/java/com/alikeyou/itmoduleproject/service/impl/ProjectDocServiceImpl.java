package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectDocCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectDocUpdateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectDoc;
import com.alikeyou.itmoduleproject.entity.ProjectDocVersion;
import com.alikeyou.itmoduleproject.repository.ProjectDocRepository;
import com.alikeyou.itmoduleproject.repository.ProjectDocVersionRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectDocService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.ProjectDocListItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionCompareVO;
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

    private static final List<String> DOC_TYPES = List.of("wiki", "spec", "meeting_note", "design", "manual", "other");
    private static final List<String> DOC_STATUSES = List.of("draft", "published", "archived");
    private static final List<String> DOC_VISIBILITIES = List.of("project", "team", "private");

    private final ProjectDocRepository projectDocRepository;
    private final ProjectDocVersionRepository projectDocVersionRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocListItemVO> listDocs(Long projectId, String type, String keyword, String status, String visibility, String isPrimary, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectDocRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream()
                .filter(doc -> matchType(doc, type))
                .filter(doc -> matchKeyword(doc, keyword))
                .filter(doc -> matchStatus(doc, status))
                .filter(doc -> matchVisibility(doc, visibility))
                .filter(doc -> matchPrimary(doc, isPrimary))
                .sorted(docComparator())
                .map(this::toListItemVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocListItemVO> listSidebarDocs(Long projectId, Long currentUserId) {
        return listDocs(projectId, null, null, null, null, null, currentUserId).stream().limit(6).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVO getPrimaryReadmeDoc(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectDoc primary = projectDocRepository.findFirstByProjectIdAndIsPrimaryTrueOrderByUpdatedAtDesc(projectId).orElse(null);
        if (primary != null && StringUtils.hasText(primary.getCurrentContent())) {
            return toDocVO(primary);
        }
        return projectDocRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream()
                .filter(this::canBePrimaryReadme)
                .sorted(primaryReadmeComparator())
                .map(this::toDocVO)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public ProjectDocVO createDoc(Long projectId, ProjectDocCreateRequest request, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        validateRequest(request);

        ProjectDoc doc = new ProjectDoc();
        doc.setProjectId(projectId);
        doc.setTitle(request.getTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setStatus(normalizeStatus(request.getStatus()));
        doc.setVisibility(normalizeVisibility(request.getVisibility()));
        doc.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        doc.setCurrentContent(normalizeContent(request.getContent()));
        doc.setCurrentVersion(1);
        doc.setCreatorId(currentUserId);
        doc.setEditorId(currentUserId);

        if (Boolean.TRUE.equals(doc.getIsPrimary())) {
            clearProjectPrimaryDoc(projectId, null);
        }

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved.getId(), 1, saved.getCurrentContent(), request.getChangeSummary(), currentUserId));
        projectActivityLogService.record(projectId, currentUserId, "create_doc", "doc", saved.getId(), "新建文档：" + saved.getTitle());
        if (Boolean.TRUE.equals(saved.getIsPrimary())) {
            projectActivityLogService.record(projectId, currentUserId, "set_primary_doc", "doc", saved.getId(), "设为主文档：" + saved.getTitle());
        }
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
        validateRequest(request);

        boolean nextPrimary = Boolean.TRUE.equals(request.getIsPrimary());
        if (nextPrimary) {
            clearProjectPrimaryDoc(doc.getProjectId(), doc.getId());
        }

        doc.setTitle(request.getTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setStatus(normalizeStatus(request.getStatus()));
        doc.setVisibility(normalizeVisibility(request.getVisibility()));
        doc.setIsPrimary(nextPrimary);
        doc.setCurrentContent(normalizeContent(request.getContent()));
        doc.setCurrentVersion(nextVersionNo(doc.getCurrentVersion()));
        doc.setEditorId(currentUserId);

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved.getId(), saved.getCurrentVersion(), saved.getCurrentContent(), request.getChangeSummary(), currentUserId));
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "update_doc", "doc", saved.getId(), "更新文档：" + saved.getTitle());
        if (Boolean.TRUE.equals(saved.getIsPrimary())) {
            projectActivityLogService.record(saved.getProjectId(), currentUserId, "set_primary_doc", "doc", saved.getId(), "设为主文档：" + saved.getTitle());
        }
        return toDocVO(saved);
    }

    @Override
    @Transactional
    public ProjectDocVO setPrimaryDoc(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        clearProjectPrimaryDoc(doc.getProjectId(), doc.getId());
        doc.setIsPrimary(Boolean.TRUE);
        doc.setEditorId(currentUserId);
        ProjectDoc saved = projectDocRepository.save(doc);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "set_primary_doc", "doc", saved.getId(), "设为主文档：" + saved.getTitle());
        return toDocVO(saved);
    }

    @Override
    @Transactional
    public void deleteDoc(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        projectActivityLogService.record(doc.getProjectId(), currentUserId, "delete_doc", "doc", doc.getId(), "删除文档：" + doc.getTitle());
        projectDocVersionRepository.deleteByDocId(docId);
        projectDocRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocVersionVO> listVersions(Long docId, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        Integer currentVersion = doc.getCurrentVersion();
        return projectDocVersionRepository.findByDocIdOrderByVersionNoDescCreatedAtDesc(docId)
                .stream()
                .map(item -> toVersionVO(item, doc, Objects.equals(item.getVersionNo(), currentVersion)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVersionVO getVersion(Long docId, Integer versionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        ProjectDocVersion version = getVersionEntity(docId, versionNo);
        return toVersionVO(version, doc, Objects.equals(version.getVersionNo(), doc.getCurrentVersion()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVersionCompareVO compareVersions(Long docId, Integer fromVersionNo, Integer toVersionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        if (fromVersionNo == null || toVersionNo == null) {
            throw new BusinessException("对比版本号不能为空");
        }
        ProjectDocVersion left = getVersionEntity(docId, fromVersionNo);
        ProjectDocVersion right = getVersionEntity(docId, toVersionNo);
        return ProjectDocVersionCompareVO.builder()
                .docId(doc.getId())
                .docTitle(doc.getTitle())
                .docType(doc.getDocType())
                .currentVersionNo(doc.getCurrentVersion())
                .leftVersionNo(left.getVersionNo())
                .rightVersionNo(right.getVersionNo())
                .leftContent(Objects.toString(left.getContentSnapshot(), ""))
                .rightContent(Objects.toString(right.getContentSnapshot(), ""))
                .leftEditedBy(left.getEditedBy())
                .rightEditedBy(right.getEditedBy())
                .leftChangeSummary(left.getChangeSummary())
                .rightChangeSummary(right.getChangeSummary())
                .leftCreatedAt(left.getCreatedAt())
                .rightCreatedAt(right.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProjectDocVO rollback(Long docId, Integer versionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectWritable(doc.getProjectId(), currentUserId);
        ProjectDocVersion version = getVersionEntity(docId, versionNo);

        doc.setCurrentContent(Objects.toString(version.getContentSnapshot(), ""));
        doc.setCurrentVersion(nextVersionNo(doc.getCurrentVersion()));
        doc.setEditorId(currentUserId);

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved.getId(), saved.getCurrentVersion(), saved.getCurrentContent(), "回滚到版本 " + versionNo, currentUserId));
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "rollback_doc", "doc", saved.getId(), "回滚文档：" + saved.getTitle() + " -> 版本 " + versionNo);
        return toDocVO(saved);
    }

    private ProjectDoc getDocEntity(Long docId) {
        return projectDocRepository.findById(docId).orElseThrow(() -> new BusinessException("项目文档不存在"));
    }

    private ProjectDocVersion getVersionEntity(Long docId, Integer versionNo) {
        return projectDocVersionRepository.findByDocIdAndVersionNo(docId, versionNo).orElseThrow(() -> new BusinessException("文档版本不存在"));
    }

    private void validateRequest(ProjectDocCreateRequest request) {
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

    private void clearProjectPrimaryDoc(Long projectId, Long excludeDocId) {
        projectDocRepository.findByProjectIdAndIsPrimaryTrue(projectId).forEach(item -> {
            if (excludeDocId != null && Objects.equals(item.getId(), excludeDocId)) {
                return;
            }
            if (Boolean.TRUE.equals(item.getIsPrimary())) {
                item.setIsPrimary(Boolean.FALSE);
                projectDocRepository.save(item);
            }
        });
    }

    private boolean matchType(ProjectDoc doc, String type) {
        return !StringUtils.hasText(type) || normalizeDocType(type).equalsIgnoreCase(Objects.toString(doc.getDocType(), "wiki"));
    }

    private boolean matchKeyword(ProjectDoc doc, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String s = keyword.trim().toLowerCase(Locale.ROOT);
        String title = Objects.toString(doc.getTitle(), "").toLowerCase(Locale.ROOT);
        String content = Objects.toString(doc.getCurrentContent(), "").toLowerCase(Locale.ROOT);
        return title.contains(s) || content.contains(s);
    }

    private boolean matchStatus(ProjectDoc doc, String status) {
        return !StringUtils.hasText(status) || normalizeStatus(status).equalsIgnoreCase(Objects.toString(doc.getStatus(), "draft"));
    }

    private boolean matchVisibility(ProjectDoc doc, String visibility) {
        return !StringUtils.hasText(visibility) || normalizeVisibility(visibility).equalsIgnoreCase(Objects.toString(doc.getVisibility(), "project"));
    }

    private boolean matchPrimary(ProjectDoc doc, String isPrimary) {
        if (!StringUtils.hasText(isPrimary)) {
            return true;
        }
        if ("true".equalsIgnoreCase(isPrimary)) {
            return Boolean.TRUE.equals(doc.getIsPrimary());
        }
        if ("false".equalsIgnoreCase(isPrimary)) {
            return !Boolean.TRUE.equals(doc.getIsPrimary());
        }
        return true;
    }

    private boolean canBePrimaryReadme(ProjectDoc doc) {
        if (doc == null) {
            return false;
        }
        if (!StringUtils.hasText(doc.getCurrentContent())) {
            return false;
        }
        if (Boolean.TRUE.equals(doc.getIsPrimary())) {
            return true;
        }
        if (!"published".equalsIgnoreCase(Objects.toString(doc.getStatus(), ""))) {
            return false;
        }
        if (!"project".equalsIgnoreCase(Objects.toString(doc.getVisibility(), ""))) {
            return false;
        }
        return true;
    }

    private Comparator<ProjectDoc> docComparator() {
        return Comparator
                .comparing((ProjectDoc item) -> Boolean.TRUE.equals(item.getIsPrimary())).reversed()
                .thenComparingInt(this::readmePriority).reversed()
                .thenComparing((ProjectDoc item) -> item.getUpdatedAt() == null ? item.getCreatedAt() : item.getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectDoc::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<ProjectDoc> primaryReadmeComparator() {
        return Comparator
                .comparing((ProjectDoc item) -> Boolean.TRUE.equals(item.getIsPrimary())).reversed()
                .thenComparingInt(this::readmePriority).reversed()
                .thenComparing((ProjectDoc item) -> item.getUpdatedAt() == null ? item.getCreatedAt() : item.getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectDoc::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int readmePriority(ProjectDoc doc) {
        if (doc == null) {
            return 0;
        }
        if (Boolean.TRUE.equals(doc.getIsPrimary())) {
            return 2000;
        }
        String title = Objects.toString(doc.getTitle(), "").trim().toLowerCase(Locale.ROOT);
        String docType = Objects.toString(doc.getDocType(), "").trim().toLowerCase(Locale.ROOT);

        if ("readme".equals(title) || "readme.md".equals(title)) {
            return 1000;
        }
        if ("项目说明".equals(title) || "项目文档".equals(title)) {
            return 900;
        }
        if (title.contains("readme")) {
            return 800;
        }
        if (title.contains("说明") || title.contains("介绍") || title.contains("文档")) {
            return 700;
        }

        return switch (docType) {
            case "wiki" -> 500;
            case "manual" -> 450;
            case "spec" -> 400;
            case "design" -> 350;
            case "meeting_note" -> 300;
            case "other" -> 100;
            default -> 0;
        };
    }

    private ProjectDocVersion buildVersion(Long docId, Integer versionNo, String content, String changeSummary, Long editedBy) {
        return ProjectDocVersion.builder()
                .docId(docId)
                .versionNo(versionNo)
                .contentSnapshot(normalizeContent(content))
                .changeSummary(normalizeChangeSummary(changeSummary))
                .editedBy(editedBy)
                .build();
    }

    private String normalizeDocType(String value) {
        if (!StringUtils.hasText(value)) {
            return "wiki";
        }
        String trimmed = value.trim();
        String s = trimmed.toLowerCase(Locale.ROOT);
        if ("说明文档".equals(trimmed)) return "wiki";
        if ("readme".equals(s) || "README".equals(trimmed)) return "wiki";
        if ("需求文档".equals(trimmed) || "规格文档".equals(trimmed)) return "spec";
        if ("会议纪要".equals(trimmed)) return "meeting_note";
        if ("设计文档".equals(trimmed)) return "design";
        if ("使用手册".equals(trimmed)) return "manual";
        if ("其他".equals(trimmed)) return "other";
        return DOC_TYPES.contains(s) ? s : "other";
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "draft";
        }
        String s = value.trim().toLowerCase(Locale.ROOT);
        return DOC_STATUSES.contains(s) ? s : "draft";
    }

    private String normalizeVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return "project";
        }
        String s = value.trim().toLowerCase(Locale.ROOT);
        return DOC_VISIBILITIES.contains(s) ? s : "project";
    }

    private String normalizeContent(String value) {
        return value == null ? "" : value;
    }

    private String normalizeChangeSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return "保存文档";
        }
        String s = value.trim();
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private int nextVersionNo(Integer currentVersion) {
        int base = currentVersion == null || currentVersion < 1 ? 1 : currentVersion;
        return base + 1;
    }

    private boolean isReadmeCandidate(ProjectDoc doc) {
        return Boolean.TRUE.equals(doc.getIsPrimary()) || readmePriority(doc) >= 700;
    }

    private String buildExcerpt(String content) {
        String s = Objects.toString(content, "").replace("\r", " ").replace("\n", " ").trim();
        if (s.length() <= 120) {
            return s;
        }
        return s.substring(0, 120) + "...";
    }

    private ProjectDocListItemVO toListItemVO(ProjectDoc doc) {
        return ProjectDocListItemVO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .status(doc.getStatus())
                .visibility(doc.getVisibility())
                .isPrimary(Boolean.TRUE.equals(doc.getIsPrimary()))
                .currentVersion(doc.getCurrentVersion())
                .creatorId(doc.getCreatorId())
                .editorId(doc.getEditorId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .excerpt(buildExcerpt(doc.getCurrentContent()))
                .readmeCandidate(isReadmeCandidate(doc))
                .build();
    }

    private ProjectDocVO toDocVO(ProjectDoc doc) {
        return ProjectDocVO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .status(doc.getStatus())
                .visibility(doc.getVisibility())
                .isPrimary(Boolean.TRUE.equals(doc.getIsPrimary()))
                .content(doc.getCurrentContent())
                .currentVersion(doc.getCurrentVersion())
                .creatorId(doc.getCreatorId())
                .editorId(doc.getEditorId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .readmeCandidate(isReadmeCandidate(doc))
                .readmePriority(readmePriority(doc))
                .build();
    }

    private ProjectDocVersionVO toVersionVO(ProjectDocVersion version, ProjectDoc doc, boolean currentVersion) {
        return ProjectDocVersionVO.builder()
                .id(version.getId())
                .docId(version.getDocId())
                .versionNo(version.getVersionNo())
                .contentSnapshot(version.getContentSnapshot())
                .changeSummary(version.getChangeSummary())
                .editedBy(version.getEditedBy())
                .createdAt(version.getCreatedAt())
                .docTitle(doc.getTitle())
                .docType(doc.getDocType())
                .currentVersion(currentVersion)
                .build();
    }
}
