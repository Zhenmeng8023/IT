package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenReviewRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistItemCreateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistItemUpdateRequest;
import com.alikeyou.itmoduleproject.dto.TaskChecklistSortRequest;
import com.alikeyou.itmoduleproject.dto.TaskCommentCreateRequest;
import com.alikeyou.itmoduleproject.dto.TaskDependencyCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectTaskAttachment;
import com.alikeyou.itmoduleproject.service.ProjectTaskAttachmentService;
import com.alikeyou.itmoduleproject.service.ProjectTaskChecklistService;
import com.alikeyou.itmoduleproject.service.ProjectTaskCommentService;
import com.alikeyou.itmoduleproject.service.ProjectTaskDependencyService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectTaskReopenRequestVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import com.alikeyou.itmoduleproject.vo.TaskAttachmentVO;
import com.alikeyou.itmoduleproject.vo.TaskChecklistItemVO;
import com.alikeyou.itmoduleproject.vo.TaskCommentVO;
import com.alikeyou.itmoduleproject.vo.TaskDependencyVO;
import com.alikeyou.itmoduleproject.vo.TaskLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/project/task")
@RequiredArgsConstructor
@Tag(name = "项目任务模块")
public class ProjectTaskController {
    private final ProjectTaskService projectTaskService;
    private final ProjectTaskCommentService projectTaskCommentService;
    private final ProjectTaskChecklistService projectTaskChecklistService;
    private final ProjectTaskAttachmentService projectTaskAttachmentService;
    private final ProjectTaskDependencyService projectTaskDependencyService;
    private final ProjectTaskLogService projectTaskLogService;
    private final CurrentUserProvider currentUserProvider;
    private final FileStorageService fileStorageService;

    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public ResponseEntity<ApiResponse<List<ProjectTaskVO>>> listTasks(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listTasks(projectId, status, priority, assigneeId, currentUserId)));
    }

    @GetMapping("/my")
    @Operation(summary = "我的任务")
    public ResponseEntity<ApiResponse<List<ProjectTaskVO>>> myTasks(@RequestParam Long projectId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listMyTasks(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "创建任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> createTask(@RequestBody ProjectTaskCreateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.createTask(requestBody, currentUserId)));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "修改任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTask(@PathVariable Long taskId, @RequestBody ProjectTaskUpdateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTask(taskId, requestBody, currentUserId)));
    }

    @PutMapping("/{taskId}/status")
    @Operation(summary = "修改任务状态")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTaskStatus(@PathVariable Long taskId, @RequestBody ProjectTaskStatusUpdateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTaskStatus(taskId, requestBody, currentUserId)));
    }

    @PostMapping("/{taskId}/reopen-requests")
    @Operation(summary = "提交任务重开申请")
    public ResponseEntity<ApiResponse<ProjectTaskReopenRequestVO>> applyReopenRequest(@PathVariable Long taskId, @RequestBody ProjectTaskReopenApplyRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.applyReopenRequest(taskId, requestBody, currentUserId)));
    }

    @GetMapping("/{taskId}/reopen-requests")
    @Operation(summary = "任务重开申请列表")
    public ResponseEntity<ApiResponse<List<ProjectTaskReopenRequestVO>>> listReopenRequests(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listReopenRequests(taskId, currentUserId)));
    }

    @PostMapping("/{taskId}/reopen-requests/{requestId}/approve")
    @Operation(summary = "通过任务重开申请")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> approveReopenRequest(@PathVariable Long taskId, @PathVariable Long requestId, @RequestBody(required = false) ProjectTaskReopenReviewRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.approveReopenRequest(taskId, requestId, requestBody, currentUserId)));
    }

    @PostMapping("/{taskId}/reopen-requests/{requestId}/reject")
    @Operation(summary = "驳回任务重开申请")
    public ResponseEntity<ApiResponse<ProjectTaskReopenRequestVO>> rejectReopenRequest(@PathVariable Long taskId, @PathVariable Long requestId, @RequestBody(required = false) ProjectTaskReopenReviewRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.rejectReopenRequest(taskId, requestId, requestBody, currentUserId)));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务")
    public ResponseEntity<ApiResponse<Object>> deleteTask(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskService.deleteTask(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @GetMapping("/{taskId}/comments")
    @Operation(summary = "任务评论列表")
    public ResponseEntity<ApiResponse<List<TaskCommentVO>>> listComments(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskCommentService.listComments(taskId, currentUserId)));
    }

    @PostMapping("/{taskId}/comments")
    @Operation(summary = "新增任务评论")
    public ResponseEntity<ApiResponse<TaskCommentVO>> addComment(@PathVariable Long taskId, @RequestBody TaskCommentCreateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskCommentService.addComment(taskId, requestBody, currentUserId)));
    }

    @PostMapping("/comments/{commentId}/reply")
    @Operation(summary = "回复任务评论")
    public ResponseEntity<ApiResponse<TaskCommentVO>> replyComment(@PathVariable Long commentId, @RequestBody TaskCommentCreateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskCommentService.replyComment(commentId, requestBody, currentUserId)));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除任务评论")
    public ResponseEntity<ApiResponse<Object>> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskCommentService.deleteComment(commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @GetMapping("/{taskId}/checklist")
    @Operation(summary = "任务检查项列表")
    public ResponseEntity<ApiResponse<List<TaskChecklistItemVO>>> listChecklist(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskChecklistService.listChecklist(taskId, currentUserId)));
    }

    @PostMapping("/{taskId}/checklist")
    @Operation(summary = "新增任务检查项")
    public ResponseEntity<ApiResponse<TaskChecklistItemVO>> addChecklistItem(@PathVariable Long taskId, @RequestBody TaskChecklistItemCreateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskChecklistService.addItem(taskId, requestBody, currentUserId)));
    }

    @PutMapping("/checklist/{itemId}")
    @Operation(summary = "修改任务检查项")
    public ResponseEntity<ApiResponse<TaskChecklistItemVO>> updateChecklistItem(@PathVariable Long itemId, @RequestBody TaskChecklistItemUpdateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskChecklistService.updateItem(itemId, requestBody, currentUserId)));
    }

    @PutMapping("/checklist/{itemId}/toggle")
    @Operation(summary = "勾选任务检查项")
    public ResponseEntity<ApiResponse<TaskChecklistItemVO>> toggleChecklistItem(@PathVariable Long itemId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskChecklistService.toggleItem(itemId, currentUserId)));
    }

    @DeleteMapping("/checklist/{itemId}")
    @Operation(summary = "删除任务检查项")
    public ResponseEntity<ApiResponse<Object>> deleteChecklistItem(@PathVariable Long itemId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskChecklistService.deleteItem(itemId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @PutMapping("/{taskId}/checklist/sort")
    @Operation(summary = "任务检查项排序")
    public ResponseEntity<ApiResponse<List<TaskChecklistItemVO>>> sortChecklist(@PathVariable Long taskId, @RequestBody TaskChecklistSortRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskChecklistService.sortItems(taskId, requestBody, currentUserId)));
    }

    @GetMapping("/{taskId}/attachments")
    @Operation(summary = "任务附件列表")
    public ResponseEntity<ApiResponse<List<TaskAttachmentVO>>> listAttachments(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskAttachmentService.listAttachments(taskId, currentUserId)));
    }

    @PostMapping("/{taskId}/attachments/upload")
    @Operation(summary = "上传任务附件")
    public ResponseEntity<ApiResponse<TaskAttachmentVO>> uploadAttachment(@PathVariable Long taskId, @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskAttachmentService.uploadAttachment(taskId, file, currentUserId)));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "删除任务附件")
    public ResponseEntity<ApiResponse<Object>> deleteAttachment(@PathVariable Long attachmentId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskAttachmentService.deleteAttachment(attachmentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "下载任务附件")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectTaskAttachment attachment = projectTaskAttachmentService.getAttachmentEntity(attachmentId, currentUserId);
        return buildFileResponse(attachment, false);
    }

    @GetMapping("/attachments/{attachmentId}/preview")
    @Operation(summary = "预览任务附件")
    public ResponseEntity<Resource> previewAttachment(@PathVariable Long attachmentId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectTaskAttachment attachment = projectTaskAttachmentService.getAttachmentEntity(attachmentId, currentUserId);
        return buildFileResponse(attachment, true);
    }

    @GetMapping("/{taskId}/dependencies")
    @Operation(summary = "任务依赖列表")
    public ResponseEntity<ApiResponse<List<TaskDependencyVO>>> listDependencies(@PathVariable Long taskId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskDependencyService.listDependencies(taskId, currentUserId)));
    }

    @PostMapping("/{taskId}/dependencies")
    @Operation(summary = "新增任务依赖")
    public ResponseEntity<ApiResponse<TaskDependencyVO>> addDependency(@PathVariable Long taskId, @RequestBody TaskDependencyCreateRequest requestBody, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskDependencyService.addDependency(taskId, requestBody, currentUserId)));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @Operation(summary = "删除任务依赖")
    public ResponseEntity<ApiResponse<Object>> deleteDependency(@PathVariable Long dependencyId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskDependencyService.deleteDependency(dependencyId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @GetMapping("/{taskId}/logs")
    @Operation(summary = "任务日志列表")
    public ResponseEntity<ApiResponse<List<TaskLogVO>>> listLogs(
            @PathVariable Long taskId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskLogService.listLogs(taskId, action, operatorId, startTime, endTime, currentUserId)));
    }

    private ResponseEntity<Resource> buildFileResponse(ProjectTaskAttachment attachment, boolean inline) {
        Resource resource = fileStorageService.loadAsResource(attachment.getFilePath());
        MediaType mediaType = MediaTypeFactory.getMediaType(attachment.getFileName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
