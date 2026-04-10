package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.ProjectStatDaily;
import com.alikeyou.itmoduleproject.service.ProjectStatService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project/stat")
@RequiredArgsConstructor
@Tag(name = "项目统计")
public class ProjectStatController {

    private final ProjectStatService projectStatService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/overview")
    @Operation(summary = "项目统计概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(@RequestParam Long projectId,
                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStatService.getOverview(projectId, currentUserId)));
    }

    @GetMapping("/trend")
    @Operation(summary = "项目统计趋势")
    public ResponseEntity<ApiResponse<List<ProjectStatDaily>>> trend(@RequestParam Long projectId,
                                                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStatService.getTrend(projectId, startDate, endDate, currentUserId)));
    }

    @GetMapping("/daily/page")
    @Operation(summary = "项目统计日报分页")
    public ResponseEntity<ApiResponse<PageResult<ProjectStatDaily>>> page(@RequestParam Long projectId,
                                                                          @RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStatService.pageDaily(projectId, page, size, currentUserId)));
    }

    @PostMapping("/rebuild")
    @Operation(summary = "重建项目统计日报")
    public ResponseEntity<ApiResponse<List<ProjectStatDaily>>> rebuild(@RequestParam Long projectId,
                                                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                                       HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStatService.rebuildDailyStats(projectId, startDate, endDate, currentUserId)));
    }
}
