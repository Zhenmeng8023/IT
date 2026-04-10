package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.ProjectDownloadRecord;
import com.alikeyou.itmoduleproject.service.ProjectDownloadRecordService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/project/download-record")
@RequiredArgsConstructor
@Tag(name = "项目下载记录")
public class ProjectDownloadRecordController {

    private final ProjectDownloadRecordService projectDownloadRecordService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/record")
    @Operation(summary = "记录项目下载")
    public ResponseEntity<ApiResponse<ProjectDownloadRecord>> record(@RequestParam Long projectId,
                                                                     @RequestParam(required = false) Long fileId,
                                                                     @RequestParam(required = false) Long fileVersionId,
                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        String ipAddress = httpServletRequest == null ? null : httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest == null ? null : httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(projectDownloadRecordService.recordDownload(projectId, fileId, fileVersionId, currentUserId, ipAddress, userAgent)));
    }

    @GetMapping("/page")
    @Operation(summary = "项目下载记录分页")
    public ResponseEntity<ApiResponse<PageResult<ProjectDownloadRecord>>> page(@RequestParam Long projectId,
                                                                               @RequestParam(defaultValue = "1") int page,
                                                                               @RequestParam(defaultValue = "10") int size,
                                                                               HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectDownloadRecordService.pageRecords(projectId, page, size, currentUserId)));
    }

    @GetMapping("/summary")
    @Operation(summary = "项目下载摘要")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(@RequestParam Long projectId,
                                                                    HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectDownloadRecordService.getSummary(projectId, currentUserId)));
    }
}
