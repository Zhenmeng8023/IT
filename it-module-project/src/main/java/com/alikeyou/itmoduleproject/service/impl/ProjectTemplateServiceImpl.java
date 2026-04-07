package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectTemplateApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateItemRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateSaveRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.ProjectDoc;
import com.alikeyou.itmoduleproject.entity.ProjectDocVersion;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.ProjectTemplate;
import com.alikeyou.itmoduleproject.entity.ProjectTemplateFile;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectActivityLogRepository;
import com.alikeyou.itmoduleproject.repository.ProjectDocRepository;
import com.alikeyou.itmoduleproject.repository.ProjectDocVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTemplateFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTemplateRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectTemplateService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateSelectableItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateSourceVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private static final long MAX_FILE_SNAPSHOT_BYTES = 4L * 1024L * 1024L;
    private static final String ITEM_META = "meta";
    private static final String ITEM_README = "readme";
    private static final String ITEM_DOC = "doc";
    private static final String ITEM_TASK = "task";
    private static final String ITEM_FILE = "file";
    private static final String ITEM_FOLDER = "folder";
    private static final String ITEM_ACTIVITY = "activity";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectTemplateFileRepository projectTemplateFileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectDocRepository projectDocRepository;
    private final ProjectDocVersionRepository projectDocVersionRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectActivityLogRepository projectActivityLogRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectActivityLogService projectActivityLogService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    @Transactional(readOnly = true)
    public List<ProjectTemplateVO> listTemplates(String keyword, Boolean mineOnly, Long currentUserId) {
        List<ProjectTemplate> list;
        if (Boolean.TRUE.equals(mineOnly)) {
            if (currentUserId == null) {
                return Collections.emptyList();
            }
            list = projectTemplateRepository.findByCreatorIdOrderByUpdatedAtDesc(currentUserId);
        } else if (currentUserId == null) {
            list = projectTemplateRepository.findByIsPublicTrueOrderByUpdatedAtDesc();
        } else {
            list = projectTemplateRepository.findByIsPublicTrueOrCreatorIdOrderByUpdatedAtDesc(currentUserId);
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        return list.stream()
                .filter(item -> !StringUtils.hasText(normalizedKeyword) || matchesKeyword(item, normalizedKeyword))
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectTemplateDetailVO getTemplateDetail(Long templateId, Long currentUserId) {
        ProjectTemplate template = getTemplate(templateId);
        assertTemplateReadable(template, currentUserId);
        return toDetailVO(template);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectTemplateSourceVO getTemplateSource(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        assertProjectManage(project, currentUserId);

        List<ProjectDoc> docs = projectDocRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        List<ProjectTask> tasks = projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<ProjectFile> files = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId);
        List<ProjectActivityLog> activities = projectActivityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        LinkedHashSet<String> suffixes = new LinkedHashSet<>();
        List<ProjectTemplateSelectableItemVO> fileOptions = files.stream()
                .map(file -> {
                    String ext = detectExt(file.getFileName(), file.getFilePath());
                    if (StringUtils.hasText(ext)) {
                        suffixes.add(ext);
                    }
                    return ProjectTemplateSelectableItemVO.builder()
                            .id(file.getId())
                            .itemType(ITEM_FILE)
                            .name(file.getFileName())
                            .path(file.getFilePath())
                            .fileExt(ext)
                            .fileSize(defaultLong(file.getFileSizeBytes()))
                            .subType(file.getFileType())
                            .status(Boolean.TRUE.equals(file.getIsMain()) ? "main" : "normal")
                            .summary(buildFileSummary(file))
                            .createdAt(file.getUploadTime())
                            .build();
                })
                .toList();

        LinkedHashSet<String> actions = new LinkedHashSet<>();
        List<ProjectTemplateSelectableItemVO> activityOptions = activities.stream()
                .map(activity -> {
                    if (StringUtils.hasText(activity.getAction())) {
                        actions.add(activity.getAction());
                    }
                    return ProjectTemplateSelectableItemVO.builder()
                            .id(activity.getId())
                            .itemType(ITEM_ACTIVITY)
                            .name(activity.getContent())
                            .path(firstText(activity.getTargetType(), "project") + ":" + Optional.ofNullable(activity.getTargetId()).orElse(0L))
                            .action(activity.getAction())
                            .summary(activity.getContent())
                            .createdAt(activity.getCreatedAt())
                            .build();
                })
                .toList();

        List<ProjectTemplateSelectableItemVO> docOptions = docs.stream()
                .map(doc -> ProjectTemplateSelectableItemVO.builder()
                        .id(doc.getId())
                        .itemType(Boolean.TRUE.equals(doc.getIsPrimary()) ? ITEM_README : ITEM_DOC)
                        .name(doc.getTitle())
                        .path("/docs/" + safeSegment(doc.getTitle()) + ".md")
                        .status(doc.getStatus())
                        .subType(doc.getDocType())
                        .summary(previewText(firstText(doc.getCurrentContent(), "")))
                        .createdAt(doc.getUpdatedAt())
                        .primary(Boolean.TRUE.equals(doc.getIsPrimary()))
                        .build())
                .toList();

        List<ProjectTemplateSelectableItemVO> taskOptions = tasks.stream()
                .map(task -> ProjectTemplateSelectableItemVO.builder()
                        .id(task.getId())
                        .itemType(ITEM_TASK)
                        .name(task.getTitle())
                        .path("task:" + task.getId())
                        .status(task.getStatus())
                        .subType(task.getPriority())
                        .summary(previewText(firstText(task.getDescription(), "")))
                        .createdAt(task.getCreatedAt())
                        .build())
                .toList();

        return ProjectTemplateSourceVO.builder()
                .projectId(projectId)
                .docCount(docOptions.size())
                .taskCount(taskOptions.size())
                .fileCount(fileOptions.size())
                .activityCount(activityOptions.size())
                .fileSuffixOptions(new ArrayList<>(suffixes))
                .activityActionOptions(new ArrayList<>(actions))
                .docs(docOptions)
                .tasks(taskOptions)
                .files(fileOptions)
                .activities(activityOptions)
                .build();
    }

    @Override
    @Transactional
    public ProjectTemplateVO createTemplate(ProjectTemplateCreateRequest request, Long currentUserId) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException("模板名称不能为空");
        }
        ProjectTemplate saved = projectTemplateRepository.save(ProjectTemplate.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .category(trimToNull(request.getCategory()))
                .creatorId(currentUserId)
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build());
        rebuildTemplateBlueprints(saved, request == null ? null : request.getReadmeTitle(), request == null ? null : request.getReadmeContent(), request == null ? null : request.getItems());
        return toVO(saved);
    }

    @Override
    @Transactional
    public ProjectTemplateVO updateTemplate(Long templateId, ProjectTemplateUpdateRequest request, Long currentUserId) {
        ProjectTemplate template = getTemplate(templateId);
        assertTemplateOwner(template, currentUserId);
        if (request != null) {
            if (StringUtils.hasText(request.getName())) {
                template.setName(request.getName().trim());
            }
            template.setDescription(trimToNull(request.getDescription()));
            template.setCategory(trimToNull(request.getCategory()));
            if (request.getIsPublic() != null) {
                template.setIsPublic(request.getIsPublic());
            }
        }
        ProjectTemplate saved = projectTemplateRepository.save(template);
        rebuildTemplateBlueprints(saved, request == null ? null : request.getReadmeTitle(), request == null ? null : request.getReadmeContent(), request == null ? null : request.getItems());
        return toVO(saved);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId, Long currentUserId) {
        ProjectTemplate template = getTemplate(templateId);
        assertTemplateOwner(template, currentUserId);
        projectTemplateRepository.delete(template);
    }

    @Override
    @Transactional
    public ProjectDetailVO applyTemplate(Long templateId, ProjectTemplateApplyRequest request, Long currentUserId) {
        ProjectTemplate template = getTemplate(templateId);
        assertTemplateReadable(template, currentUserId);

        String name = StringUtils.hasText(request == null ? null : request.getProjectName())
                ? request.getProjectName().trim()
                : template.getName() + "-副本";

        Project project = Project.builder()
                .name(name)
                .description(firstText(request == null ? null : request.getProjectDescription(), template.getDescription()))
                .category(firstText(request == null ? null : request.getCategory(), template.getCategory()))
                .authorId(currentUserId)
                .status("draft")
                .tags(normalizeTags(request == null ? null : request.getTags()))
                .sizeMb(BigDecimal.ZERO)
                .stars(0)
                .downloads(0)
                .views(0)
                .templateId(template.getId())
                .visibility(firstText(request == null ? null : request.getVisibility(), "public"))
                .build();
        Project savedProject = projectRepository.save(project);

        List<ProjectTemplateFile> rows = projectTemplateFileRepository.findByTemplateIdOrderBySortOrderAscIdAsc(templateId);
        Set<Long> selectedDocIds = safeIdSet(request == null ? null : request.getSelectedTemplateDocIds());
        Set<Long> selectedTaskIds = safeIdSet(request == null ? null : request.getSelectedTemplateTaskIds());
        Set<Long> selectedActivityIds = safeIdSet(request == null ? null : request.getSelectedTemplateActivityIds());
        Set<Long> selectedFileIds = safeIdSet(request == null ? null : request.getSelectedTemplateFileIds());
        Set<String> suffixFilter = normalizeSet(request == null ? null : request.getFileSuffixes());

        boolean applyDocs = request == null || request.getApplyDocs() == null || Boolean.TRUE.equals(request.getApplyDocs());
        boolean applyReadme = request == null || request.getApplyReadme() == null || Boolean.TRUE.equals(request.getApplyReadme());
        boolean applyTasks = request == null || request.getApplyTasks() == null || Boolean.TRUE.equals(request.getApplyTasks());
        boolean applyFiles = request == null || request.getApplyFiles() == null || Boolean.TRUE.equals(request.getApplyFiles());
        boolean applyActivities = request != null && Boolean.TRUE.equals(request.getApplyActivities());
        boolean createFolders = request == null || request.getCreateFolders() == null || Boolean.TRUE.equals(request.getCreateFolders());
        boolean applyDocVersions = request != null && Boolean.TRUE.equals(request.getApplyDocVersionHistory());
        String fileMode = normalizeFileMode(request == null ? null : request.getFileMode());
        String activityMode = normalizeActivityMode(request == null ? null : request.getActivityMode());

        int docCount = 0;
        int taskCount = 0;
        int fileCount = 0;
        int folderCount = 0;
        int activityCount = 0;

        for (ProjectTemplateFile row : rows) {
            String itemType = normalizeItemType(row.getItemType());
            TemplatePayload payload = readPayload(row);
            if (ITEM_META.equals(itemType)) {
                continue;
            }
            if ((ITEM_README.equals(itemType) || ITEM_DOC.equals(itemType)) && applyDocs) {
                if (!selectedDocIds.isEmpty() && !selectedDocIds.contains(row.getId())) {
                    continue;
                }
                if (ITEM_README.equals(itemType) && !applyReadme) {
                    continue;
                }
                createDocFromPayload(savedProject.getId(), currentUserId, row, payload, ITEM_README.equals(itemType), applyDocVersions);
                docCount++;
                continue;
            }
            if (ITEM_TASK.equals(itemType) && applyTasks) {
                if (!selectedTaskIds.isEmpty() && !selectedTaskIds.contains(row.getId())) {
                    continue;
                }
                createTaskFromPayload(savedProject.getId(), currentUserId, payload, request);
                taskCount++;
                continue;
            }
            if (ITEM_FOLDER.equals(itemType) && applyFiles && createFolders) {
                if (!selectedFileIds.isEmpty() && !selectedFileIds.contains(row.getId())) {
                    continue;
                }
                createFolderRecord(savedProject.getId(), row, payload);
                folderCount++;
                continue;
            }
            if (ITEM_FILE.equals(itemType) && applyFiles) {
                if (!selectedFileIds.isEmpty() && !selectedFileIds.contains(row.getId())) {
                    continue;
                }
                if (!suffixFilter.isEmpty() && !suffixFilter.contains(normalizeExt(row.getFileExt()))) {
                    continue;
                }
                createFileRecord(savedProject.getId(), row, payload, fileMode);
                fileCount++;
                continue;
            }
            if (ITEM_ACTIVITY.equals(itemType) && applyActivities && !"skip".equals(activityMode)) {
                if (!selectedActivityIds.isEmpty() && !selectedActivityIds.contains(row.getId())) {
                    continue;
                }
                createActivityFromPayload(savedProject.getId(), currentUserId, row, payload, activityMode);
                activityCount++;
            }
        }

        if (docCount == 0 && StringUtils.hasText(template.getDescription())) {
            createFallbackReadme(savedProject.getId(), currentUserId, template.getName(), template.getDescription());
            docCount++;
        }

        projectActivityLogService.record(savedProject.getId(), currentUserId, "create_project", "project", savedProject.getId(), "创建项目：" + savedProject.getName());
        projectActivityLogService.record(savedProject.getId(), currentUserId, "apply_template", "project", savedProject.getId(),
                "从模板创建项目：" + template.getName() + "（文档" + docCount + "，任务" + taskCount + "，文件" + fileCount + "，目录" + folderCount + "，活动流" + activityCount + "）");

        return ProjectDetailVO.builder()
                .id(savedProject.getId())
                .name(savedProject.getName())
                .description(savedProject.getDescription())
                .category(savedProject.getCategory())
                .sizeMb(savedProject.getSizeMb())
                .stars(savedProject.getStars())
                .downloads(savedProject.getDownloads())
                .views(savedProject.getViews())
                .authorId(savedProject.getAuthorId())
                .status(savedProject.getStatus())
                .tags(savedProject.getTags())
                .templateId(savedProject.getTemplateId())
                .visibility(savedProject.getVisibility())
                .createdAt(savedProject.getCreatedAt())
                .updatedAt(savedProject.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProjectTemplateVO saveProjectAsTemplate(Long projectId, ProjectTemplateSaveRequest request, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        assertProjectManage(project, currentUserId);

        String templateName = firstText(request == null ? null : request.getName(), project.getName());
        if (!StringUtils.hasText(templateName)) {
            throw new BusinessException("模板名称不能为空");
        }

        ProjectTemplate template = projectTemplateRepository.save(ProjectTemplate.builder()
                .name(templateName.trim())
                .description(firstText(request == null ? null : request.getDescription(), project.getDescription()))
                .category(firstText(request == null ? null : request.getCategory(), project.getCategory()))
                .creatorId(currentUserId)
                .isPublic(Boolean.TRUE.equals(request != null && request.getIsPublic()))
                .build());

        projectTemplateFileRepository.deleteByTemplateId(template.getId());

        List<ProjectTemplateFile> rows = new ArrayList<>();
        int sort = 0;
        SnapshotMeta meta = new SnapshotMeta();
        meta.projectId = projectId;
        meta.projectName = project.getName();
        meta.savedAt = LocalDateTime.now();
        meta.fileMode = normalizeFileMode(request == null ? null : request.getFileMode());
        meta.fileSuffixes = normalizeSortedList(request == null ? null : request.getFileSuffixes());
        meta.activityActions = normalizeSortedList(request == null ? null : request.getSelectedActivityActions());
        meta.includeReadme = request == null || request.getIncludeReadme() == null || Boolean.TRUE.equals(request.getIncludeReadme());
        meta.includeDocVersionHistory = request != null && Boolean.TRUE.equals(request.getIncludeDocVersionHistory());
        meta.includeTaskDescription = request == null || request.getIncludeTaskDescription() == null || Boolean.TRUE.equals(request.getIncludeTaskDescription());
        meta.includeTaskChecklist = request != null && Boolean.TRUE.equals(request.getIncludeTaskChecklist());
        meta.includeTaskAttachments = request != null && Boolean.TRUE.equals(request.getIncludeTaskAttachments());
        meta.includeTaskDependencies = request != null && Boolean.TRUE.equals(request.getIncludeTaskDependencies());

        boolean includeDocs = request == null || request.getIncludeDocs() == null || Boolean.TRUE.equals(request.getIncludeDocs());
        boolean includeTasks = request == null || request.getIncludeTasks() == null || Boolean.TRUE.equals(request.getIncludeTasks());
        boolean includeFiles = request != null && Boolean.TRUE.equals(request.getIncludeFiles());
        boolean includeActivities = request != null && Boolean.TRUE.equals(request.getIncludeActivities());

        if (includeDocs) {
            Set<Long> selectedDocIds = safeIdSet(request == null ? null : request.getSelectedDocIds());
            List<ProjectDoc> docs = projectDocRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
            for (ProjectDoc doc : docs) {
                if (!selectedDocIds.isEmpty() && !selectedDocIds.contains(doc.getId())) {
                    continue;
                }
                if (Boolean.TRUE.equals(doc.getIsPrimary()) && !meta.includeReadme) {
                    continue;
                }
                TemplatePayload payload = buildDocSnapshot(doc, meta.includeDocVersionHistory);
                rows.add(buildTemplateRow(template.getId(), Boolean.TRUE.equals(doc.getIsPrimary()) ? ITEM_README : ITEM_DOC,
                        "docs", doc.getId(), "doc:" + doc.getId(), payload, false, sort++));
                meta.docCount++;
                if (Boolean.TRUE.equals(doc.getIsPrimary())) {
                    meta.readmeTitle = payload.name;
                }
            }
        }

        if (includeTasks) {
            Set<Long> selectedTaskIds = safeIdSet(request == null ? null : request.getSelectedTaskIds());
            List<ProjectTask> tasks = projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                    .stream()
                    .sorted(Comparator.comparing(ProjectTask::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (ProjectTask task : tasks) {
                if (!selectedTaskIds.isEmpty() && !selectedTaskIds.contains(task.getId())) {
                    continue;
                }
                TemplatePayload payload = buildTaskSnapshot(task, request);
                rows.add(buildTemplateRow(template.getId(), ITEM_TASK, "tasks", task.getId(), "task:" + task.getId(), payload, false, sort++));
                meta.taskCount++;
            }
        }

        if (includeFiles) {
            Set<Long> selectedFileIds = safeIdSet(request == null ? null : request.getSelectedFileIds());
            Set<String> suffixFilter = normalizeSet(request == null ? null : request.getFileSuffixes());
            boolean includeContent = "structure_and_content".equals(meta.fileMode);
            List<ProjectFile> files = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId)
                    .stream()
                    .sorted(Comparator.comparing(ProjectFile::getUploadTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            LinkedHashMap<String, TemplatePayload> folderPayloads = new LinkedHashMap<>();
            for (ProjectFile file : files) {
                if (!selectedFileIds.isEmpty() && !selectedFileIds.contains(file.getId())) {
                    continue;
                }
                String ext = detectExt(file.getFileName(), file.getFilePath());
                if (!suffixFilter.isEmpty() && !suffixFilter.contains(normalizeExt(ext))) {
                    continue;
                }
                collectFolders(folderPayloads, file.getFilePath());
                TemplatePayload payload = buildFileSnapshot(file, includeContent);
                rows.add(buildTemplateRow(template.getId(), ITEM_FILE, "files", file.getId(), "file:" + file.getId(), payload, includeContent, sort++));
                meta.fileCount++;
                if (StringUtils.hasText(ext)) {
                    meta.fileSuffixes.add(ext);
                }
            }
            for (Map.Entry<String, TemplatePayload> entry : folderPayloads.entrySet()) {
                rows.add(buildTemplateRow(template.getId(), ITEM_FOLDER, "files", null, "folder:" + entry.getKey(), entry.getValue(), false, sort++));
                meta.folderCount++;
            }
        }

        if (includeActivities) {
            Set<Long> selectedActivityIds = safeIdSet(request == null ? null : request.getSelectedActivityIds());
            Set<String> selectedActions = normalizeSet(request == null ? null : request.getSelectedActivityActions());
            List<ProjectActivityLog> activities = projectActivityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            for (ProjectActivityLog activity : activities) {
                if (!selectedActivityIds.isEmpty() && !selectedActivityIds.contains(activity.getId())) {
                    continue;
                }
                if (!selectedActions.isEmpty() && !selectedActions.contains(normalizeToken(activity.getAction()))) {
                    continue;
                }
                if (request != null && request.getActivityStartTime() != null && activity.getCreatedAt() != null && activity.getCreatedAt().isBefore(request.getActivityStartTime())) {
                    continue;
                }
                if (request != null && request.getActivityEndTime() != null && activity.getCreatedAt() != null && activity.getCreatedAt().isAfter(request.getActivityEndTime())) {
                    continue;
                }
                TemplatePayload payload = buildActivitySnapshot(activity, projectId, project.getName());
                rows.add(buildTemplateRow(template.getId(), ITEM_ACTIVITY, "activities", activity.getId(), "activity:" + activity.getId(), payload, false, sort++));
                meta.activityCount++;
                if (StringUtils.hasText(activity.getAction())) {
                    meta.activityActions.add(activity.getAction());
                }
            }
        }

        rows.add(0, buildMetaRow(template.getId(), meta));

        if (!rows.isEmpty()) {
            projectTemplateFileRepository.saveAll(rows);
        }

        projectActivityLogService.record(projectId, currentUserId, "save_as_template", "project", projectId,
                "保存项目模板：" + template.getName() + "（文档" + meta.docCount + "，任务" + meta.taskCount + "，文件" + meta.fileCount + "，目录" + meta.folderCount + "，活动流" + meta.activityCount + "）");
        return toVO(template);
    }

    private String normalizeExt(String ext) {
        if (ext == null) {
            return "";
        }
        String s = ext.trim().toLowerCase();
        if (s.startsWith(".")) {
            s = s.substring(1);
        }
        return s;
    }

    private boolean isBinaryExt(String ext) {
        String s = normalizeExt(ext);
        switch (s) {
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
            case "mp4":
            case "avi":
            case "mov":
            case "mkv":
            case "pdf":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "zip":
            case "rar":
            case "7z":
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "bmp":
            case "webp":
                return true;
            default:
                return false;
        }
    }

    private String resolveFileMimeType(String ext, String rawType) {
        if (rawType != null && !rawType.trim().isEmpty()) {
            return rawType.trim();
        }

        String s = normalizeExt(ext);
        switch (s) {
            case "txt":
                return "text/plain";
            case "md":
                return "text/markdown";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "ts":
                return "application/typescript";
            case "json":
                return "application/json";
            case "java":
                return "text/x-java-source";
            case "xml":
                return "application/xml";
            case "yml":
            case "yaml":
                return "application/x-yaml";
            case "sql":
                return "application/sql";
            case "vue":
                return "text/plain";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }

    private void rebuildTemplateBlueprints(ProjectTemplate template, String readmeTitle, String readmeContent, List<ProjectTemplateItemRequest> items) {
        projectTemplateFileRepository.deleteByTemplateId(template.getId());
        List<ProjectTemplateFile> rows = new ArrayList<>();
        int sort = 0;
        SnapshotMeta meta = new SnapshotMeta();
        meta.savedAt = LocalDateTime.now();

        if (StringUtils.hasText(readmeTitle) || StringUtils.hasText(readmeContent)) {
            TemplatePayload payload = new TemplatePayload();
            payload.name = firstText(readmeTitle, template.getName() + " README");
            payload.path = "/docs/README.md";
            payload.content = firstText(readmeContent, template.getDescription());
            payload.docType = "wiki";
            payload.status = "draft";
            payload.visibility = "project";
            payload.isPrimary = true;
            payload.version = "1.0";
            rows.add(buildTemplateRow(template.getId(), ITEM_README, "docs", null, "manual:readme", payload, false, sort++));
            meta.docCount++;
            meta.readmeTitle = payload.name;
        }

        if (items != null) {
            List<ProjectTemplateItemRequest> normalized = items.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(item -> Optional.ofNullable(item.getSortOrder()).orElse(0)))
                    .toList();
            for (ProjectTemplateItemRequest item : normalized) {
                String itemType = normalizeItemType(item.getItemType());
                TemplatePayload payload = buildPayload(itemType, item);
                if (!hasMeaningfulPayload(itemType, payload)) {
                    continue;
                }
                rows.add(buildTemplateRow(template.getId(), itemType, normalizeGroup(itemType), null,
                        itemType + ":manual:" + safeSegment(firstText(item.getName(), itemType)), payload, ITEM_FILE.equals(itemType), sort++));
                if (ITEM_DOC.equals(itemType) || ITEM_README.equals(itemType)) {
                    meta.docCount++;
                } else if (ITEM_TASK.equals(itemType)) {
                    meta.taskCount++;
                } else if (ITEM_FILE.equals(itemType)) {
                    meta.fileCount++;
                }
            }
        }

        rows.add(0, buildMetaRow(template.getId(), meta));
        if (!rows.isEmpty()) {
            projectTemplateFileRepository.saveAll(rows);
        }
    }

    private ProjectTemplateFile buildTemplateRow(Long templateId, String itemType, String groupName, Long sourceId, String itemKey, TemplatePayload payload, boolean includeContent, int sortOrder) {
        String fileExt = detectExt(payload.name, payload.path);
        if (!StringUtils.hasText(fileExt)) {
            if (ITEM_FOLDER.equals(itemType)) {
                fileExt = "folder";
            } else if (ITEM_TASK.equals(itemType)) {
                fileExt = "task";
            } else if (ITEM_ACTIVITY.equals(itemType)) {
                fileExt = "activity";
            } else {
                fileExt = "md";
            }
        }
        return ProjectTemplateFile.builder()
                .templateId(templateId)
                .itemType(itemType)
                .itemKey(itemKey)
                .groupName(groupName)
                .sourceId(sourceId)
                .fileName(firstText(payload.name, itemType))
                .filePath(firstText(payload.path, "/" + itemType + "/" + safeSegment(firstText(payload.name, itemType))))
                .fileExt(fileExt)
                .fileSize(defaultLong(payload.fileSize))
                .mimeType(firstText(payload.mimeType, resolveMimeType(itemType, payload)))
                .includeContent(includeContent)
                .version(firstText(payload.version, "1.0"))
                .sortOrder(sortOrder)
                .payloadJson(writePayload(payload))
                .build();
    }

    private ProjectTemplateFile buildMetaRow(Long templateId, SnapshotMeta meta) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = "模板保存配置";
        payload.path = "/meta/template.json";
        payload.meta = objectToMap(meta);
        payload.content = writeValue(payload.meta);
        payload.mimeType = "application/json";
        payload.version = "1.0";
        return ProjectTemplateFile.builder()
                .templateId(templateId)
                .itemType(ITEM_META)
                .itemKey("meta:config")
                .groupName("meta")
                .fileName(payload.name)
                .filePath(payload.path)
                .fileExt("json")
                .fileSize((long) payload.content.length())
                .mimeType(payload.mimeType)
                .includeContent(Boolean.TRUE)
                .version("1.0")
                .sortOrder(-1)
                .payloadJson(writePayload(payload))
                .build();
    }

    private TemplatePayload buildDocSnapshot(ProjectDoc doc, boolean includeHistory) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = firstText(doc.getTitle(), "文档");
        payload.path = "/docs/" + safeSegment(payload.name) + ".md";
        payload.content = firstText(doc.getCurrentContent(), "");
        payload.docType = firstText(doc.getDocType(), "wiki");
        payload.status = firstText(doc.getStatus(), "draft");
        payload.visibility = firstText(doc.getVisibility(), "project");
        payload.isPrimary = Boolean.TRUE.equals(doc.getIsPrimary());
        payload.version = String.valueOf(Optional.ofNullable(doc.getCurrentVersion()).orElse(1));
        payload.mimeType = "text/markdown";
        payload.fileSize = (long) payload.content.length();
        if (includeHistory) {
            try {
                List<ProjectDocVersion> versions = projectDocVersionRepository.findByDocIdOrderByVersionNoDescCreatedAtDesc(doc.getId());
                payload.docVersions = versions.stream().map(version -> {
                    DocVersionPayload vp = new DocVersionPayload();
                    vp.versionNo = version.getVersionNo();
                    vp.contentSnapshot = version.getContentSnapshot();
                    vp.changeSummary = version.getChangeSummary();
                    vp.editedBy = version.getEditedBy();
                    vp.createdAt = version.getCreatedAt();
                    return vp;
                }).toList();
            } catch (Exception ignored) {
            }
        }
        return payload;
    }

    private TemplatePayload buildTaskSnapshot(ProjectTask task, ProjectTemplateSaveRequest request) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = firstText(task.getTitle(), "任务");
        payload.path = "/tasks/" + safeSegment(payload.name) + ".json";
        String text = request == null || request.getIncludeTaskDescription() == null || Boolean.TRUE.equals(request.getIncludeTaskDescription())
                ? firstText(task.getDescription(), "")
                : "";
        payload.content = text == null ? "" : text;
        payload.status = firstText(task.getStatus(), "todo");
        payload.priority = firstText(task.getPriority(), "medium");
        payload.version = "1.0";
        payload.mimeType = "application/json";
        payload.fileSize = (long) payload.content.length();
        Map<String, Object> taskMeta = new LinkedHashMap<>();
        taskMeta.put("includeChecklist", request != null && Boolean.TRUE.equals(request.getIncludeTaskChecklist()));
        taskMeta.put("includeAttachments", request != null && Boolean.TRUE.equals(request.getIncludeTaskAttachments()));
        taskMeta.put("includeDependencies", request != null && Boolean.TRUE.equals(request.getIncludeTaskDependencies()));
        payload.meta = taskMeta;
        return payload;
    }

    private TemplatePayload buildFileSnapshot(ProjectFile file, boolean includeContent) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = firstText(file.getFileName(), "文件");
        payload.path = normalizeFilePath(file.getFilePath(), payload.name);
        payload.fileType = firstText(file.getFileType(), "file");
        payload.version = firstText(file.getVersion(), "1.0");
        String ext = detectExt(file.getFileName(), file.getFilePath());
        payload.mimeType = resolveFileMimeType(ext, file.getFileType());
        payload.fileSize = defaultLong(file.getFileSizeBytes());
        payload.isMainFile = Boolean.TRUE.equals(file.getIsMain());
        payload.meta = new LinkedHashMap<>();
        payload.meta.put("sourceFilePath", file.getFilePath());
        payload.meta.put("sourceUploadTime", file.getUploadTime());
        payload.meta.put("fileExt", ext);
        if (includeContent && !isBinaryExt(ext)) {
            SnapshotContent snapshotContent = readFileSnapshot(file.getFilePath(), payload.path);
            payload.contentBase64 = snapshotContent.base64;
            payload.meta.put("contentStored", snapshotContent.stored);
            payload.meta.put("contentReason", snapshotContent.reason);
        } else {
            payload.meta.put("contentStored", false);
            payload.meta.put("contentReason", includeContent ? "二进制文件未写入模板内容" : "仅保存结构");
        }
        return payload;
    }

    private TemplatePayload buildActivitySnapshot(ProjectActivityLog activity, Long sourceProjectId, String sourceProjectName) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = firstText(activity.getContent(), firstText(activity.getAction(), "模板活动流"));
        payload.path = "/activities/" + Optional.ofNullable(activity.getId()).orElse(0L) + ".json";
        payload.content = firstText(activity.getContent(), "");
        payload.action = activity.getAction();
        payload.targetType = activity.getTargetType();
        payload.targetId = activity.getTargetId();
        payload.createdAt = activity.getCreatedAt();
        payload.detailsJson = activity.getDetails();
        payload.meta = new LinkedHashMap<>();
        payload.meta.put("sourceProjectId", sourceProjectId);
        payload.meta.put("sourceProjectName", sourceProjectName);
        payload.meta.put("originalCreatedAt", activity.getCreatedAt());
        payload.meta.put("originalOperatorId", activity.getOperatorId());
        return payload;
    }

    private void createDocFromPayload(Long projectId, Long currentUserId, ProjectTemplateFile row, TemplatePayload payload, boolean forcePrimary, boolean applyDocVersions) {
        ProjectDoc doc = projectDocRepository.save(ProjectDoc.builder()
                .projectId(projectId)
                .title(firstText(payload.name, row.getFileName()))
                .docType(firstText(payload.docType, "wiki"))
                .status(firstText(payload.status, "draft"))
                .visibility(firstText(payload.visibility, "project"))
                .isPrimary(forcePrimary || Boolean.TRUE.equals(payload.isPrimary))
                .currentContent(firstText(payload.content, ""))
                .currentVersion(1)
                .creatorId(currentUserId)
                .editorId(currentUserId)
                .build());
        projectDocVersionRepository.save(ProjectDocVersion.builder()
                .docId(doc.getId())
                .versionNo(1)
                .contentSnapshot(firstText(payload.content, ""))
                .changeSummary("从模板初始化")
                .editedBy(currentUserId)
                .build());
        if (applyDocVersions && payload.docVersions != null && payload.docVersions.size() > 1) {
            List<DocVersionPayload> history = payload.docVersions.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(item -> Optional.ofNullable(item.versionNo).orElse(0)))
                    .toList();
            for (DocVersionPayload version : history) {
                if (Objects.equals(version.versionNo, 1)) {
                    continue;
                }
                projectDocVersionRepository.save(ProjectDocVersion.builder()
                        .docId(doc.getId())
                        .versionNo(version.versionNo)
                        .contentSnapshot(firstText(version.contentSnapshot, payload.content))
                        .changeSummary(firstText(version.changeSummary, "从模板导入历史版本"))
                        .editedBy(firstLong(version.editedBy, currentUserId))
                        .build());
            }
            doc.setCurrentVersion(history.stream().map(item -> Optional.ofNullable(item.versionNo).orElse(1)).max(Integer::compareTo).orElse(1));
            DocVersionPayload latest = history.get(history.size() - 1);
            doc.setCurrentContent(firstText(latest.contentSnapshot, doc.getCurrentContent()));
            doc.setEditorId(firstLong(latest.editedBy, currentUserId));
            projectDocRepository.save(doc);
        }
    }

    private void createTaskFromPayload(Long projectId, Long currentUserId, TemplatePayload payload, ProjectTemplateApplyRequest request) {
        boolean includeDescription = request == null || request.getApplyTaskDescription() == null || Boolean.TRUE.equals(request.getApplyTaskDescription());
        projectTaskRepository.save(ProjectTask.builder()
                .projectId(projectId)
                .title(firstText(payload.name, "待办任务"))
                .description(includeDescription ? firstText(payload.content, "") : "")
                .status(firstText(payload.status, "todo"))
                .priority(firstText(payload.priority, "medium"))
                .assigneeId(null)
                .createdBy(currentUserId)
                .build());
    }

    private void createFolderRecord(Long projectId, ProjectTemplateFile row, TemplatePayload payload) {
        projectFileRepository.save(ProjectFile.builder()
                .projectId(projectId)
                .fileName(firstText(payload.name, row.getFileName()))
                .filePath(firstText(payload.path, row.getFilePath()))
                .fileSizeBytes(0L)
                .fileType("folder")
                .isMain(false)
                .version(firstText(payload.version, "1.0"))
                .isLatest(true)
                .build());
    }

    private void createFileRecord(Long projectId, ProjectTemplateFile row, TemplatePayload payload, String requestedMode) {
        boolean shouldWriteContent = "structure_and_content".equals(requestedMode) && Boolean.TRUE.equals(row.getIncludeContent()) && StringUtils.hasText(payload.contentBase64);
        Path target = resolveTemplateWritePath(projectId, firstText(payload.path, row.getFilePath()), firstText(payload.name, row.getFileName()));
        byte[] data = new byte[0];
        if (shouldWriteContent) {
            try {
                data = Base64.getDecoder().decode(payload.contentBase64);
            } catch (IllegalArgumentException ignored) {
                data = new byte[0];
            }
        }
        try {
            Files.createDirectories(target.getParent());
            if (shouldWriteContent) {
                Files.write(target, data);
            } else if (!Files.exists(target)) {
                Files.write(target, new byte[0]);
            }
        } catch (IOException ignored) {
        }
        projectFileRepository.save(ProjectFile.builder()
                .projectId(projectId)
                .fileName(firstText(payload.name, row.getFileName()))
                .filePath(target.toString().replace('\\', '/'))
                .fileSizeBytes((long) data.length)
                .fileType(firstText(payload.fileType, normalizeExt(row.getFileExt())))
                .isMain(Boolean.TRUE.equals(payload.isMainFile))
                .version(firstText(payload.version, "1.0"))
                .isLatest(true)
                .build());
    }

    private void createActivityFromPayload(Long projectId, Long currentUserId, ProjectTemplateFile row, TemplatePayload payload, String activityMode) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("templateImported", true);
        details.put("templateMode", activityMode);
        details.put("source", payload.meta);
        details.put("originalDetails", tryReadJson(payload.detailsJson));
        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(projectId)
                .operatorId(currentUserId)
                .action(firstText(payload.action, "template_activity_import"))
                .targetType(firstText(payload.targetType, "project"))
                .targetId(payload.targetId)
                .content(("import_as_template_history".equals(activityMode) ? "[模板历史] " : "[模板导入] ") + firstText(payload.content, row.getFileName()))
                .details(writeValue(details))
                .build());
    }

    private void createFallbackReadme(Long projectId, Long currentUserId, String title, String content) {
        ProjectDoc doc = projectDocRepository.save(ProjectDoc.builder()
                .projectId(projectId)
                .title(firstText(title, "项目说明"))
                .docType("wiki")
                .status("draft")
                .visibility("project")
                .isPrimary(Boolean.TRUE)
                .currentContent(firstText(content, ""))
                .currentVersion(1)
                .creatorId(currentUserId)
                .editorId(currentUserId)
                .build());
        projectDocVersionRepository.save(ProjectDocVersion.builder()
                .docId(doc.getId())
                .versionNo(1)
                .contentSnapshot(firstText(content, ""))
                .changeSummary("模板初始化")
                .editedBy(currentUserId)
                .build());
    }

    private ProjectTemplate getTemplate(Long templateId) {
        return projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("模板不存在"));
    }

    private void assertTemplateReadable(ProjectTemplate template, Long currentUserId) {
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (Boolean.TRUE.equals(template.getIsPublic())) {
            return;
        }
        if (currentUserId == null || !Objects.equals(template.getCreatorId(), currentUserId)) {
            throw new BusinessException("无权查看该模板");
        }
    }

    private void assertTemplateOwner(ProjectTemplate template, Long currentUserId) {
        if (template == null || currentUserId == null || !Objects.equals(template.getCreatorId(), currentUserId)) {
            throw new BusinessException("仅模板创建者可操作");
        }
    }

    private void assertProjectManage(Project project, Long currentUserId) {
        if (project == null || currentUserId == null) {
            throw new BusinessException("无权操作");
        }
        if (Objects.equals(project.getAuthorId(), currentUserId)) {
            return;
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId).orElse(null);
        String role = member == null ? "" : String.valueOf(member.getRole()).toLowerCase(Locale.ROOT);
        String status = member == null ? "" : String.valueOf(member.getStatus()).toLowerCase(Locale.ROOT);
        if (!"active".equals(status) || (!"owner".equals(role) && !"admin".equals(role))) {
            throw new BusinessException("仅项目所有者或管理员可操作");
        }
    }

    private boolean matchesKeyword(ProjectTemplate item, String keyword) {
        return contains(item.getName(), keyword) || contains(item.getDescription(), keyword) || contains(item.getCategory(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String normalizeToken(String token) {
        return StringUtils.hasText(token) ? token.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private Long firstLong(Long first, Long second) {
        return first != null ? first : second;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeTags(String tags) {
        return StringUtils.hasText(tags) ? tags.trim() : null;
    }

    private String normalizeItemType(String itemType) {
        String value = StringUtils.hasText(itemType) ? itemType.trim().toLowerCase(Locale.ROOT) : ITEM_DOC;
        if (Set.of(ITEM_META, ITEM_README, ITEM_DOC, ITEM_TASK, ITEM_FILE, ITEM_FOLDER, ITEM_ACTIVITY).contains(value)) {
            return value;
        }
        return ITEM_DOC;
    }

    private String normalizeGroup(String itemType) {
        return switch (normalizeItemType(itemType)) {
            case ITEM_DOC, ITEM_README -> "docs";
            case ITEM_TASK -> "tasks";
            case ITEM_FILE, ITEM_FOLDER -> "files";
            case ITEM_ACTIVITY -> "activities";
            default -> "meta";
        };
    }

    private String normalizeFileMode(String fileMode) {
        String value = normalizeToken(fileMode);
        if ("structure_and_content".equals(value) || "structure_only".equals(value) || "none".equals(value)) {
            return value;
        }
        return "structure_only";
    }

    private String normalizeActivityMode(String activityMode) {
        String value = normalizeToken(activityMode);
        if ("import_as_template_history".equals(value) || "import_as_new_log".equals(value) || "skip".equals(value)) {
            return value;
        }
        return "skip";
    }

    private String resolveMimeType(String itemType, TemplatePayload payload) {
        if (ITEM_TASK.equals(itemType)) {
            return "application/json";
        }
        if (ITEM_ACTIVITY.equals(itemType)) {
            return "application/json";
        }
        if (ITEM_FOLDER.equals(itemType)) {
            return "application/x-folder";
        }
        if (ITEM_FILE.equals(itemType)) {
            return firstText(payload.mimeType, "application/octet-stream");
        }
        return "text/markdown";
    }

    private TemplatePayload buildPayload(String itemType, ProjectTemplateItemRequest item) {
        TemplatePayload payload = new TemplatePayload();
        payload.name = firstText(item.getName(), itemType.equals(ITEM_TASK) ? "任务" : "内容项");
        payload.path = firstText(item.getPath(), defaultPath(itemType, payload.name));
        payload.content = firstText(item.getContent(), "");
        payload.docType = firstText(item.getDocType(), "wiki");
        payload.status = firstText(item.getStatus(), itemType.equals(ITEM_TASK) ? "todo" : "draft");
        payload.visibility = firstText(item.getVisibility(), "project");
        payload.isPrimary = Boolean.TRUE.equals(item.getIsPrimary());
        payload.priority = firstText(item.getPriority(), "medium");
        payload.version = firstText(item.getVersion(), "1.0");
        payload.fileType = firstText(item.getFileType(), itemType.equals(ITEM_FILE) ? "template_placeholder" : null);
        payload.mimeType = resolveMimeType(itemType, payload);
        payload.fileSize = (long) firstText(payload.content, "").length();
        return payload;
    }

    private boolean hasMeaningfulPayload(String itemType, TemplatePayload payload) {
        if (payload == null) {
            return false;
        }
        if (ITEM_TASK.equals(itemType)) {
            return StringUtils.hasText(payload.name) || StringUtils.hasText(payload.content);
        }
        if (ITEM_FILE.equals(itemType) || ITEM_FOLDER.equals(itemType)) {
            return StringUtils.hasText(payload.name) || StringUtils.hasText(payload.path);
        }
        return StringUtils.hasText(payload.name) || StringUtils.hasText(payload.content);
    }

    private String defaultPath(String itemType, String name) {
        String safe = safeSegment(firstText(name, itemType));
        if (ITEM_TASK.equals(itemType)) {
            return "/tasks/" + safe + ".json";
        }
        if (ITEM_FILE.equals(itemType)) {
            return "/skeleton/" + safe;
        }
        if (ITEM_FOLDER.equals(itemType)) {
            return "/skeleton/" + safe + "/";
        }
        if (ITEM_ACTIVITY.equals(itemType)) {
            return "/activities/" + safe + ".json";
        }
        return "/docs/" + safe + ".md";
    }

    private String writePayload(TemplatePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("模板内容序列化失败");
        }
    }

    private TemplatePayload readPayload(ProjectTemplateFile row) {
        if (row == null) {
            return new TemplatePayload();
        }
        if (StringUtils.hasText(row.getPayloadJson())) {
            try {
                return objectMapper.readValue(row.getPayloadJson(), TemplatePayload.class);
            } catch (Exception ignored) {
            }
        }
        TemplatePayload payload = new TemplatePayload();
        payload.name = row.getFileName();
        payload.path = row.getFilePath();
        payload.version = row.getVersion();
        payload.fileType = row.getMimeType();
        payload.mimeType = row.getMimeType();
        payload.content = "";
        payload.docType = "wiki";
        payload.status = ITEM_TASK.equals(normalizeItemType(row.getItemType())) ? "todo" : "draft";
        payload.visibility = "project";
        payload.priority = "medium";
        payload.fileSize = row.getFileSize();
        return payload;
    }

    private String previewTextForPayload(String itemType, TemplatePayload payload) {
        if (payload == null) {
            return "";
        }
        if (ITEM_TASK.equals(itemType)) {
            return firstText(payload.content, firstText(payload.priority, ""));
        }
        if (ITEM_FILE.equals(itemType) || ITEM_FOLDER.equals(itemType)) {
            return firstText(payload.path, firstText(payload.fileType, ""));
        }
        if (ITEM_ACTIVITY.equals(itemType)) {
            return firstText(payload.content, firstText(payload.action, ""));
        }
        return firstText(payload.content, firstText(payload.docType, ""));
    }

    private String previewText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private String previewTextForVo(String itemType, TemplatePayload payload) {
        return previewText(previewTextForPayload(itemType, payload));
    }

    private String safeSegment(String value) {
        String raw = firstText(value, "item");
        return raw.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
    }

    private ProjectTemplateVO toVO(ProjectTemplate item) {
        UserInfoLite creator = userInfoLiteRepository.findById(item.getCreatorId()).orElse(null);
        List<ProjectTemplateFile> rows = projectTemplateFileRepository.findByTemplateIdOrderBySortOrderAscIdAsc(item.getId());
        Counts counts = countRows(rows);
        return ProjectTemplateVO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .creatorId(item.getCreatorId())
                .creatorName(resolveName(creator))
                .isPublic(Boolean.TRUE.equals(item.getIsPublic()))
                .itemCount(counts.itemCount)
                .docCount(counts.docCount)
                .taskCount(counts.taskCount)
                .fileCount(counts.fileCount)
                .folderCount(counts.folderCount)
                .activityCount(counts.activityCount)
                .readmeTitle(counts.readmeTitle)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private ProjectTemplateDetailVO toDetailVO(ProjectTemplate item) {
        UserInfoLite creator = userInfoLiteRepository.findById(item.getCreatorId()).orElse(null);
        List<ProjectTemplateFile> rows = projectTemplateFileRepository.findByTemplateIdOrderBySortOrderAscIdAsc(item.getId());
        Counts counts = countRows(rows);
        List<ProjectTemplateFileVO> all = rows.stream()
                .map(file -> toFileVO(file, readPayload(file)))
                .toList();
        List<ProjectTemplateFileVO> docs = all.stream().filter(file -> ITEM_DOC.equals(file.getItemType()) || ITEM_README.equals(file.getItemType())).toList();
        List<ProjectTemplateFileVO> tasks = all.stream().filter(file -> ITEM_TASK.equals(file.getItemType())).toList();
        List<ProjectTemplateFileVO> files = all.stream().filter(file -> ITEM_FILE.equals(file.getItemType())).toList();
        List<ProjectTemplateFileVO> folders = all.stream().filter(file -> ITEM_FOLDER.equals(file.getItemType())).toList();
        List<ProjectTemplateFileVO> activities = all.stream().filter(file -> ITEM_ACTIVITY.equals(file.getItemType())).toList();
        return ProjectTemplateDetailVO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .creatorId(item.getCreatorId())
                .creatorName(resolveName(creator))
                .isPublic(Boolean.TRUE.equals(item.getIsPublic()))
                .itemCount(counts.itemCount)
                .docCount(counts.docCount)
                .taskCount(counts.taskCount)
                .fileCount(counts.fileCount)
                .folderCount(counts.folderCount)
                .activityCount(counts.activityCount)
                .readmeTitle(counts.readmeTitle)
                .readmeContent(counts.readmeContent)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .savedFileSuffixes(counts.savedFileSuffixes)
                .savedActivityActions(counts.savedActivityActions)
                .files(all)
                .docItems(docs)
                .taskItems(tasks)
                .fileItems(files)
                .folderItems(folders)
                .activityItems(activities)
                .build();
    }

    private ProjectTemplateFileVO toFileVO(ProjectTemplateFile file, TemplatePayload payload) {
        return ProjectTemplateFileVO.builder()
                .id(file.getId())
                .templateId(file.getTemplateId())
                .itemType(normalizeItemType(file.getItemType()))
                .itemKey(file.getItemKey())
                .groupName(file.getGroupName())
                .sourceId(file.getSourceId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileExt(file.getFileExt())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .includeContent(Boolean.TRUE.equals(file.getIncludeContent()))
                .version(file.getVersion())
                .sortOrder(file.getSortOrder())
                .payloadJson(file.getPayloadJson())
                .previewText(previewTextForVo(normalizeItemType(file.getItemType()), payload))
                .createdAt(file.getCreatedAt())
                .build();
    }

    private Counts countRows(List<ProjectTemplateFile> rows) {
        Counts counts = new Counts();
        counts.itemCount = rows == null ? 0 : rows.size();
        if (rows == null) {
            return counts;
        }
        for (ProjectTemplateFile row : rows) {
            String type = normalizeItemType(row.getItemType());
            TemplatePayload payload = readPayload(row);
            if (ITEM_META.equals(type)) {
                SnapshotMeta meta = tryReadMeta(payload.meta);
                if (meta != null) {
                    counts.savedFileSuffixes = meta.fileSuffixes == null ? Collections.emptyList() : meta.fileSuffixes;
                    counts.savedActivityActions = meta.activityActions == null ? Collections.emptyList() : meta.activityActions;
                }
                continue;
            }
            if (ITEM_README.equals(type)) {
                counts.docCount++;
                counts.readmeTitle = firstText(payload.name, row.getFileName());
                counts.readmeContent = firstText(payload.content, "");
                continue;
            }
            if (ITEM_DOC.equals(type)) {
                counts.docCount++;
                continue;
            }
            if (ITEM_TASK.equals(type)) {
                counts.taskCount++;
                continue;
            }
            if (ITEM_FILE.equals(type)) {
                counts.fileCount++;
                continue;
            }
            if (ITEM_FOLDER.equals(type)) {
                counts.folderCount++;
                continue;
            }
            if (ITEM_ACTIVITY.equals(type)) {
                counts.activityCount++;
            }
        }
        return counts;
    }

    private SnapshotMeta tryReadMeta(Map<String, Object> metaMap) {
        if (metaMap == null || metaMap.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.convertValue(metaMap, SnapshotMeta.class);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveName(UserInfoLite user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "用户" + user.getId();
    }

    private String buildFileSummary(ProjectFile file) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(file.getFileType())) {
            parts.add(file.getFileType());
        }
        String ext = detectExt(file.getFileName(), file.getFilePath());
        if (StringUtils.hasText(ext)) {
            parts.add(ext);
        }
        if (file.getFileSizeBytes() != null) {
            parts.add(file.getFileSizeBytes() + " bytes");
        }
        return String.join(" / ", parts);
    }

    private String detectExt(String fileName, String filePath) {
        String raw = firstText(fileName, filePath);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
        String name = slash >= 0 ? raw.substring(slash + 1) : raw;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return normalizeExt(name.substring(dot + 1));
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Set<Long> safeIdSet(Collection<Long> ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeSet(Collection<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeToken)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> normalizeSortedList(Collection<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private void collectFolders(Map<String, TemplatePayload> folderPayloads, String originalPath) {
        if (!StringUtils.hasText(originalPath)) {
            return;
        }
        String normalized = originalPath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash <= 0) {
            return;
        }
        String parent = normalized.substring(0, slash);
        String[] parts = parent.split("/");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            builder.append('/').append(part);
            String path = builder.toString();
            folderPayloads.computeIfAbsent(path, key -> {
                TemplatePayload payload = new TemplatePayload();
                payload.name = part;
                payload.path = path + "/";
                payload.fileType = "folder";
                payload.version = "1.0";
                payload.fileSize = 0L;
                return payload;
            });
        }
    }

    private SnapshotContent readFileSnapshot(String rawPath, String fallbackPath) {
        SnapshotContent result = new SnapshotContent();
        Path path = resolveExistingPath(rawPath, fallbackPath);
        if (path == null) {
            result.stored = false;
            result.reason = "源文件不存在或路径不可读";
            return result;
        }
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_SNAPSHOT_BYTES) {
                result.stored = false;
                result.reason = "文件超过 " + MAX_FILE_SNAPSHOT_BYTES + " bytes，未写入模板内容";
                return result;
            }
            byte[] bytes = Files.readAllBytes(path);
            result.base64 = Base64.getEncoder().encodeToString(bytes);
            result.stored = true;
            result.reason = "ok";
            return result;
        } catch (IOException e) {
            result.stored = false;
            result.reason = "读取文件失败：" + e.getMessage();
            return result;
        }
    }

    private Path resolveExistingPath(String rawPath, String fallbackPath) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(rawPath)) {
            candidates.add(rawPath.trim());
        }
        if (StringUtils.hasText(fallbackPath)) {
            candidates.add(fallbackPath.trim());
        }
        for (String candidate : candidates) {
            Path direct = Paths.get(candidate);
            if (Files.exists(direct)) {
                return direct;
            }
            String normalized = candidate.startsWith("/") ? candidate.substring(1) : candidate;
            Path underUserDir = Paths.get(System.getProperty("user.dir"), normalized);
            if (Files.exists(underUserDir)) {
                return underUserDir;
            }
        }
        return null;
    }

    private Path resolveTemplateWritePath(Long projectId, String desiredPath, String fileName) {
        String normalized = normalizeFilePath(desiredPath, fileName);
        String relative = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        return Paths.get(System.getProperty("user.dir"), "runtime", "template-generated", "project-" + projectId, relative);
    }

    private String normalizeFilePath(String filePath, String fileName) {
        if (StringUtils.hasText(filePath)) {
            return filePath.replace('\\', '/');
        }
        return "/template-files/" + safeSegment(firstText(fileName, "item"));
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> objectToMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }

    private Object tryReadJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ignored) {
            return json;
        }
    }



    private static class SnapshotContent {
        private String base64;
        private boolean stored;
        private String reason;
    }

    private static class TemplatePayload {
        public String name;
        public String path;
        public String content;
        public String contentBase64;
        public String docType;
        public String status;
        public String visibility;
        public Boolean isPrimary;
        public String priority;
        public String version;
        public String fileType;
        public String mimeType;
        public Long fileSize;
        public Boolean isMainFile;
        public String action;
        public String targetType;
        public Long targetId;
        public LocalDateTime createdAt;
        public String detailsJson;
        public List<DocVersionPayload> docVersions;
        public Map<String, Object> meta;
    }

    private static class DocVersionPayload {
        public Integer versionNo;
        public String contentSnapshot;
        public String changeSummary;
        public Long editedBy;
        public LocalDateTime createdAt;
    }

    private static class SnapshotMeta {
        public Long projectId;
        public String projectName;
        public LocalDateTime savedAt;
        public String fileMode;
        public List<String> fileSuffixes = new ArrayList<>();
        public List<String> activityActions = new ArrayList<>();
        public Boolean includeReadme;
        public Boolean includeDocVersionHistory;
        public Boolean includeTaskDescription;
        public Boolean includeTaskChecklist;
        public Boolean includeTaskAttachments;
        public Boolean includeTaskDependencies;
        public int docCount;
        public int taskCount;
        public int fileCount;
        public int folderCount;
        public int activityCount;
        public String readmeTitle;
    }

    private static class Counts {
        private int itemCount;
        private int docCount;
        private int taskCount;
        private int fileCount;
        private int folderCount;
        private int activityCount;
        private String readmeTitle;
        private String readmeContent;
        private List<String> savedFileSuffixes = Collections.emptyList();
        private List<String> savedActivityActions = Collections.emptyList();
    }
}
