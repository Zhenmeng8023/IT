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

    private static final List<String> DOC_TYPES = List.of("wiki", "spec", "meeting_note", "design", "manual", "other");
    private static final List<String> DOC_STATUSES = List.of("draft", "published", "archived");
    private static final List<String> DOC_VISIBILITIES = List.of("project", "team", "private");

    private final ProjectDocRepository projectDocRepository;
    private final ProjectDocVersionRepository projectDocVersionRepository;
    private final ProjectPermissionService projectPermissionService;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocListItemVO> listDocs(Long projectId, String type, String keyword, String status, String visibility, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectDocRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream()
                .filter(doc -> matchType(doc, type))
                .filter(doc -> matchKeyword(doc, keyword))
                .filter(doc -> matchStatus(doc, status))
                .filter(doc -> matchVisibility(doc, visibility))
                .sorted(docComparator())
                .map(this::toListItemVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVO getPrimaryReadmeDoc(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
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
        doc.setCurrentContent(normalizeContent(request.getContent()));
        doc.setCurrentVersion(1);
        doc.setCreatorId(currentUserId);
        doc.setEditorId(currentUserId);

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved.getId(), 1, saved.getCurrentContent(), request.getChangeSummary(), currentUserId));
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

        doc.setTitle(request.getTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setStatus(normalizeStatus(request.getStatus()));
        doc.setVisibility(normalizeVisibility(request.getVisibility()));
        doc.setCurrentContent(normalizeContent(request.getContent()));
        doc.setCurrentVersion(nextVersionNo(doc.getCurrentVersion()));
        doc.setEditorId(currentUserId);

        ProjectDoc saved = projectDocRepository.save(doc);
        projectDocVersionRepository.save(buildVersion(saved.getId(), saved.getCurrentVersion(), saved.getCurrentContent(), request.getChangeSummary(), currentUserId));
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
                .map(item -> toVersionVO(item, doc))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocVersionVO getVersion(Long docId, Integer versionNo, Long currentUserId) {
        ProjectDoc doc = getDocEntity(docId);
        projectPermissionService.assertProjectReadable(doc.getProjectId(), currentUserId);
        ProjectDocVersion version = getVersionEntity(docId, versionNo);
        return toVersionVO(version, doc);
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

    private boolean canBePrimaryReadme(ProjectDoc doc) {
        if (doc == null) {
            return false;
        }
        if (!"published".equalsIgnoreCase(Objects.toString(doc.getStatus(), ""))) {
            return false;
        }
        if (!"project".equalsIgnoreCase(Objects.toString(doc.getVisibility(), ""))) {
            return false;
        }
        return StringUtils.hasText(doc.getCurrentContent());
    }

    private Comparator<ProjectDoc> docComparator() {
        return Comparator
                .comparingInt(this::readmePriority).reversed()
                .thenComparing((ProjectDoc item) -> item.getUpdatedAt() == null ? item.getCreatedAt() : item.getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectDoc::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<ProjectDoc> primaryReadmeComparator() {
        return Comparator
                .comparingInt(this::readmePriority).reversed()
                .thenComparing((ProjectDoc item) -> item.getUpdatedAt() == null ? item.getCreatedAt() : item.getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectDoc::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int readmePriority(ProjectDoc doc) {
        if (doc == null) {
            return 0;
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
        String s = value.trim().toLowerCase(Locale.ROOT);
        if ("readme".equals(s) || "说明文档".equals(value.trim())) {
            return "wiki";
        }
        if ("需求文档".equals(value.trim()) || "规格文档".equals(value.trim())) {
            return "spec";
        }
        if ("会议纪要".equals(value.trim())) {
            return "meeting_note";
        }
        if ("设计文档".equals(value.trim())) {
            return "design";
        }
        if ("使用手册".equals(value.trim())) {
            return "manual";
        }
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
        return readmePriority(doc) >= 700;
    }

    private String buildExcerpt(String content) {
        String s = Objects.toString(content, "")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
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

    private ProjectDocVersionVO toVersionVO(ProjectDocVersion version, ProjectDoc doc) {
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
                .build();
    }
}