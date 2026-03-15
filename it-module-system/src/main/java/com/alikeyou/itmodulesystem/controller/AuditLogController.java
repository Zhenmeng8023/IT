package com.alikeyou.itmodulesystem.controller;

import com.alikeyou.itmodulesystem.entity.AuditLog;
import com.alikeyou.itmodulesystem.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器
 * 处理与系统审计日志相关的 HTTP 请求
 */
@Tag(name = "审计日志管理", description = "系统操作审计日志的查询和统计操作")
@RestController
@RequestMapping("/api/system/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 记录审计日志
     * POST /api/system/audit/log
     */
    @Operation(summary = "记录审计日志", description = "记录一次系统操作到审计日志中")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功记录审计日志",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditLog.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PostMapping("/log")
    public ResponseEntity<AuditLog> logAction(
            @Parameter(description = "操作类型", required = true, example = "CREATE_BLOG")
            @RequestParam String action,
            @Parameter(description = "目标类型", example = "blog")
            @RequestParam(required = false) String targetType,
            @Parameter(description = "目标 ID", example = "1")
            @RequestParam(required = false) Long targetId,
            @Parameter(description = "IP 地址", example = "192.168.1.1")
            @RequestParam(required = false) String ipAddress,
            @Parameter(description = "用户代理")
            @RequestParam(required = false) String userAgent) {

        Map<String, Object> details = new HashMap<>();
        AuditLog auditLog = auditLogService.logAction(
                action, targetType, targetId, ipAddress, userAgent, details);
        return new ResponseEntity<>(auditLog, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 获取审计日志
     * GET /api/system/audit/{id}
     */
    @Operation(summary = "根据 ID 获取审计日志", description = "根据审计日志 ID 获取详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditLog.class))),
            @ApiResponse(responseCode = "404", description = "审计日志不存在",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(
            @Parameter(description = "审计日志 ID", required = true, example = "1")
            @PathVariable Long id) {
        return auditLogService.getAuditLogById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 获取所有审计日志
     * GET /api/system/audit
     */
    @Operation(summary = "获取所有审计日志", description = "获取所有的审计日志记录列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = AuditLog.class))))
    })
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> auditLogs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 根据操作类型查询审计日志
     * GET /api/system/audit/action/{action}
     */
    @Operation(summary = "按操作类型查询审计日志", description = "根据操作类型查询审计日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = AuditLog.class))))
    })
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(
            @Parameter(description = "操作类型", required = true, example = "CREATE_BLOG")
            @PathVariable String action) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByAction(action);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 根据目标查询审计日志
     * GET /api/system/audit/target/{targetType}/{targetId}
     */
    @Operation(summary = "按目标查询审计日志", description = "根据目标类型和目标 ID 查询审计日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = AuditLog.class))))
    })
    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByTarget(
            @Parameter(description = "目标类型", required = true, example = "blog")
            @PathVariable String targetType,
            @Parameter(description = "目标 ID", required = true, example = "1")
            @PathVariable Long targetId) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByTarget(targetType, targetId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 按时间范围查询审计日志
     * GET /api/system/audit/range
     */
    @Operation(summary = "按时间范围查询审计日志", description = "根据时间范围查询审计日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = AuditLog.class))))
    })
    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByTimeRange(
            @Parameter(description = "开始时间", required = true, example = "2024-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "结束时间", required = true, example = "2024-12-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByTimeRange(startTime, endTime);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 搜索审计日志
     * GET /api/system/audit/search
     */
    @Operation(summary = "搜索审计日志", description = "根据多个条件组合搜索审计日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取审计日志列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = AuditLog.class))))
    })
    @GetMapping("/search")
    public ResponseEntity<List<AuditLog>> searchAuditLogs(
            @Parameter(description = "操作类型", example = "CREATE_BLOG")
            @RequestParam(required = false) String action,
            @Parameter(description = "目标类型", example = "blog")
            @RequestParam(required = false) String targetType,
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        List<AuditLog> auditLogs = auditLogService.searchAuditLogs(action, targetType, startTime, endTime);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 删除审计日志
     * DELETE /api/system/audit/{id}
     */
    @Operation(summary = "删除审计日志", description = "根据审计日志 ID 删除指定的审计日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除审计日志",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "审计日志不存在",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuditLog(
            @Parameter(description = "审计日志 ID", required = true, example = "1")
            @PathVariable Long id) {
        auditLogService.deleteAuditLog(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 统计操作数量
     * GET /api/system/audit/count/action/{action}
     */
    @Operation(summary = "统计操作数量", description = "统计某个操作类型的审计日志数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功统计操作数量",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "integer")))
    })
    @GetMapping("/count/action/{action}")
    public ResponseEntity<Long> countByAction(
            @Parameter(description = "操作类型", required = true, example = "CREATE_BLOG")
            @PathVariable String action) {
        long count = auditLogService.countByAction(action);
        return ResponseEntity.ok(count);
    }

    /**
     * 统计目标类型操作数量
     * GET /api/system/audit/count/target/{targetType}
     */
    @Operation(summary = "统计目标类型操作数量", description = "统计某个目标类型的审计日志数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功统计操作数量",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "integer")))
    })
    @GetMapping("/count/target/{targetType}")
    public ResponseEntity<Long> countByTargetType(
            @Parameter(description = "目标类型", required = true, example = "blog")
            @PathVariable String targetType) {
        long count = auditLogService.countByTargetType(targetType);
        return ResponseEntity.ok(count);
    }
}
