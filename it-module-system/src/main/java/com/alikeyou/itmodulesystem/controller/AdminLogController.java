package com.alikeyou.itmodulesystem.controller;

import com.alikeyou.itmodulesystem.entity.AuditLog;
import com.alikeyou.itmodulesystem.repository.AuditLogRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final AuditLogRepository auditLogRepository;

    public AdminLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/operations/page")
    public ResponseEntity<Map<String, Object>> getOperationLogsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        Instant start = parseInstant(firstNonBlank(startTime, startDate), false);
        Instant end = parseInstant(firstNonBlank(endTime, endDate), true);

        List<Map<String, Object>> filtered = filterAndMapLogs(type, operator, module, start, end);
        int fromIndex = Math.min((safePage - 1) * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        List<Map<String, Object>> pageList = filtered.subList(fromIndex, toIndex);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", pageList);
        data.put("total", filtered.size());
        data.put("page", safePage);
        data.put("size", safeSize);

        return ResponseEntity.ok(success(data));
    }

    @GetMapping("/operations/{id}")
    public ResponseEntity<Map<String, Object>> getOperationLogDetail(@PathVariable Long id) {
        Optional<AuditLog> optional = auditLogRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.status(404).body(failure("日志不存在"));
        }
        Map<String, Object> item = mapLog(optional.get());
        return ResponseEntity.ok(success(item));
    }

    @GetMapping("/operations/export")
    public ResponseEntity<byte[]> exportOperationLogs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Instant start = parseInstant(firstNonBlank(startTime, startDate), false);
        Instant end = parseInstant(firstNonBlank(endTime, endDate), true);
        List<Map<String, Object>> logs = filterAndMapLogs(type, operator, module, start, end);

        StringBuilder builder = new StringBuilder();
        builder.append("ID,操作时间,操作人员,操作类型,操作模块,操作内容,IP,结果,详情\n");
        for (Map<String, Object> row : logs) {
            builder.append(csv(row.get("id"))).append(',')
                    .append(csv(row.get("createTime"))).append(',')
                    .append(csv(row.get("operator"))).append(',')
                    .append(csv(row.get("type"))).append(',')
                    .append(csv(row.get("module"))).append(',')
                    .append(csv(row.get("action"))).append(',')
                    .append(csv(row.get("ip"))).append(',')
                    .append(csv(row.get("result"))).append(',')
                    .append(csv(row.get("details"))).append('\n');
        }

        byte[] utf8Bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[utf8Bom.length + csvBytes.length];
        System.arraycopy(utf8Bom, 0, output, 0, utf8Bom.length);
        System.arraycopy(csvBytes, 0, output, utf8Bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operation-logs.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(output);
    }

    @DeleteMapping("/clean")
    public ResponseEntity<Map<String, Object>> cleanExpiredLogs(@RequestBody(required = false) Map<String, Object> payload) {
        int retainDays = parseInt(payload == null ? null : payload.get("retainDays"), 90);
        if (retainDays < 1) {
            retainDays = 1;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(retainDays));
        List<AuditLog> expired = auditLogRepository.findByCreatedAtBefore(cutoff);
        if (!expired.isEmpty()) {
            auditLogRepository.deleteAllInBatch(expired);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deletedCount", expired.size());
        data.put("retainDays", retainDays);
        return ResponseEntity.ok(success(data));
    }

    private List<Map<String, Object>> filterAndMapLogs(String type, String operator, String module, Instant start, Instant end) {
        String typeFilter = normalizeLower(type);
        String operatorFilter = normalizeLower(operator);
        String moduleFilter = normalizeLower(module);

        return auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapLog)
                .filter(item -> matchesDate(item.get("createTime"), start, end))
                .filter(item -> typeFilter == null || typeFilter.equals(normalizeLower(item.get("type"))))
                .filter(item -> operatorFilter == null || containsIgnoreCase(item.get("operator"), operatorFilter))
                .filter(item -> moduleFilter == null || containsIgnoreCase(item.get("module"), moduleFilter))
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapLog(AuditLog log) {
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

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", log.getId());
        item.put("createTime", log.getCreatedAt());
        item.put("operator", operator);
        item.put("type", type);
        item.put("module", module);
        item.put("action", action);
        item.put("ip", valueAsText(log.getIpAddress()));
        item.put("result", result);
        item.put("details", details.isEmpty() ? "" : details);
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

    private boolean matchesDate(Object value, Instant start, Instant end) {
        Instant current = toInstant(value);
        if (current == null) {
            return false;
        }
        if (start != null && current.isBefore(start)) {
            return false;
        }
        return end == null || !current.isAfter(end);
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
        }
        return null;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return parseInstant(String.valueOf(value), false);
    }

    private boolean containsIgnoreCase(Object value, String keyword) {
        if (keyword == null) {
            return true;
        }
        return normalizeLower(value).contains(keyword);
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

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String valueAsText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        return String.valueOf(value);
    }

    private String csv(Object value) {
        String text = valueAsText(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("data", data);
        body.put("message", "ok");
        return body;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 1);
        body.put("message", message);
        return body;
    }
}
