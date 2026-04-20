package com.alikeyou.itmodulesystem.controller;

import com.alikeyou.itmodulesystem.dto.AdminLogResponseDTO;
import com.alikeyou.itmodulesystem.dto.CleanExpiredLogsRequestDTO;
import com.alikeyou.itmodulesystem.dto.CleanExpiredLogsResultDTO;
import com.alikeyou.itmodulesystem.dto.OperationLogItemDTO;
import com.alikeyou.itmodulesystem.dto.OperationLogQueryDTO;
import com.alikeyou.itmodulesystem.dto.PageDTO;
import com.alikeyou.itmodulesystem.entity.AuditLog;
import com.alikeyou.itmodulesystem.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private static final int DEFAULT_RETAIN_DAYS = 90;

    private final AuditLogRepository auditLogRepository;

    public AdminLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/operations/page")
    public ResponseEntity<AdminLogResponseDTO<List<OperationLogItemDTO>>> getOperationLogsPage(
            @ModelAttribute OperationLogQueryDTO query
    ) {
        int safePage = query.getPage() == null ? 1 : Math.max(query.getPage(), 1);
        int safeSize = query.getSize() == null ? 20 : Math.max(query.getSize(), 1);

        Instant start = parseInstant(query.getStartTime(), false);
        Instant end = parseInstant(query.getEndTime(), true);

        Page<AuditLog> logsPage = auditLogRepository.searchAdminLogs(
                normalizeLower(query.getType()),
                normalizeLower(query.getOperator()),
                normalizeLower(query.getModule()),
                start,
                end,
                PageRequest.of(safePage - 1, safeSize)
        );

        List<OperationLogItemDTO> list = logsPage.getContent().stream()
                .map(this::mapLog)
                .toList();

        PageDTO pageDTO = new PageDTO(safePage, safeSize, logsPage.getTotalElements());
        return ResponseEntity.ok(AdminLogResponseDTO.success(list, pageDTO));
    }

    @GetMapping("/operations/{id}")
    public ResponseEntity<AdminLogResponseDTO<OperationLogItemDTO>> getOperationLogDetail(@PathVariable Long id) {
        Optional<AuditLog> optional = auditLogRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.status(404).body(AdminLogResponseDTO.failure("log not found"));
        }
        return ResponseEntity.ok(AdminLogResponseDTO.success(mapLog(optional.get())));
    }

    @GetMapping("/operations/export")
    public ResponseEntity<byte[]> exportOperationLogs(@ModelAttribute OperationLogQueryDTO query) {
        Instant start = parseInstant(query.getStartTime(), false);
        Instant end = parseInstant(query.getEndTime(), true);

        List<AuditLog> logs = auditLogRepository.searchAdminLogsForExport(
                normalizeLower(query.getType()),
                normalizeLower(query.getOperator()),
                normalizeLower(query.getModule()),
                start,
                end
        );

        StringBuilder builder = new StringBuilder();
        builder.append("ID,Time,Operator,Type,Module,Action,IP,Result,Details\n");
        for (AuditLog log : logs) {
            OperationLogItemDTO row = mapLog(log);
            builder.append(csv(row.getId())).append(',')
                    .append(csv(row.getCreateTime())).append(',')
                    .append(csv(row.getOperator())).append(',')
                    .append(csv(row.getType())).append(',')
                    .append(csv(row.getModule())).append(',')
                    .append(csv(row.getAction())).append(',')
                    .append(csv(row.getIp())).append(',')
                    .append(csv(row.getResult())).append(',')
                    .append(csv(row.getDetails()))
                    .append('\n');
        }

        byte[] utf8Bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[utf8Bom.length + csvBytes.length];
        System.arraycopy(utf8Bom, 0, output, 0, utf8Bom.length);
        System.arraycopy(csvBytes, 0, output, utf8Bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operation-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(output);
    }

    @DeleteMapping("/clean")
    public ResponseEntity<AdminLogResponseDTO<CleanExpiredLogsResultDTO>> cleanExpiredLogs(
            @RequestBody(required = false) CleanExpiredLogsRequestDTO payload
    ) {
        int retainDays = payload == null || payload.getRetainDays() == null
                ? DEFAULT_RETAIN_DAYS
                : payload.getRetainDays();
        if (retainDays < 1) {
            retainDays = 1;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retainDays));
        long deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        CleanExpiredLogsResultDTO data = new CleanExpiredLogsResultDTO(deletedCount, retainDays);
        return ResponseEntity.ok(AdminLogResponseDTO.success(data));
    }

    private OperationLogItemDTO mapLog(AuditLog log) {
        Map<String, Object> details = log.getDetails() == null ? Collections.emptyMap() : log.getDetails();
        String action = valueAsText(log.getAction());
        String type = resolveType(action, details);
        String module = firstNonBlank(
                valueAsText(details.get("module")),
                valueAsText(details.get("moduleName")),
                valueAsText(log.getTargetType()),
                "system"
        );
        String result = resolveResult(action, details);
        String operator = resolveOperator(log, details);

        OperationLogItemDTO item = new OperationLogItemDTO();
        item.setId(log.getId());
        item.setCreateTime(log.getCreatedAt());
        item.setOperator(operator);
        item.setType(type);
        item.setModule(module);
        item.setAction(action);
        item.setIp(valueAsText(log.getIpAddress()));
        item.setResult(result);
        item.setDetails(details.isEmpty() ? "" : details);
        return item;
    }

    private String resolveOperator(AuditLog log, Map<String, Object> details) {
        String fromDetails = firstNonBlank(
                valueAsText(details.get("operator")),
                valueAsText(details.get("username")),
                valueAsText(details.get("userName"))
        );
        if (fromDetails != null) {
            return fromDetails;
        }
        Long userId = log.getUserId();
        return userId == null ? "" : String.valueOf(userId);
    }

    private String resolveType(String action, Map<String, Object> details) {
        String explicit = normalizeLower(details.get("type"));
        if (explicit != null && Set.of("user", "system", "security", "error").contains(explicit)) {
            return explicit;
        }
        String lowerAction = normalizeLower(action);
        if (lowerAction == null) {
            return "system";
        }
        if (lowerAction.contains("login") || lowerAction.contains("password") || lowerAction.contains("auth")) {
            return "security";
        }
        if (lowerAction.contains("error") || lowerAction.contains("exception") || lowerAction.contains("fail")) {
            return "error";
        }
        if (lowerAction.contains("create") || lowerAction.contains("update") || lowerAction.contains("delete")) {
            return "user";
        }
        return "system";
    }

    private String resolveResult(String action, Map<String, Object> details) {
        String explicit = normalizeLower(details.get("result"));
        if (explicit == null) {
            explicit = normalizeLower(details.get("status"));
        }
        if (explicit != null) {
            if (Set.of("success", "ok", "true", "1", "passed", "pass").contains(explicit)) {
                return "success";
            }
            if (Set.of("failed", "fail", "error", "false", "0").contains(explicit)) {
                return "failed";
            }
        }
        String actionLower = normalizeLower(action);
        if (actionLower != null && (actionLower.contains("fail") || actionLower.contains("error"))) {
            return "failed";
        }
        return "success";
    }

    private Instant parseInstant(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDateTime dateTime = endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
                return dateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
            return Instant.parse(value);
        } catch (Exception ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeLower(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return text.isEmpty() ? null : text;
    }

    private String valueAsText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String csv(Object value) {
        String text = valueAsText(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
