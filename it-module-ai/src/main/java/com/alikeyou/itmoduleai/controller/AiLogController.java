package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.service.AiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/logs")
@RequiredArgsConstructor
public class AiLogController {

    private final AiLogService aiLogService;

    @PostMapping("/call")
    public ApiResponse<AiCallLog> saveCall(@RequestBody AiCallLog entity) {
        return ApiResponse.ok("记录成功", aiLogService.saveCallLog(entity));
    }

    @PostMapping("/feedback")
    public ApiResponse<AiFeedbackLog> saveFeedback(@RequestBody AiFeedbackCreateRequest request) {
        return ApiResponse.ok("反馈成功", aiLogService.saveFeedback(request));
    }

    @GetMapping("/user/{userId}/calls")
    public ApiResponse<Page<AiCallLog>> pageUserCalls(@PathVariable Long userId, Pageable pageable) {
        return ApiResponse.ok(aiLogService.pageUserCallLogs(userId, pageable));
    }

    @GetMapping("/session/{sessionId}/calls")
    public ApiResponse<Page<AiCallLog>> pageSessionCalls(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiLogService.pageSessionCallLogs(sessionId, pageable));
    }

    @GetMapping("/call/{callLogId}/retrievals")
    public ApiResponse<List<AiRetrievalLog>> listRetrievals(@PathVariable Long callLogId) {
        return ApiResponse.ok(aiLogService.listRetrievalLogs(callLogId));
    }

    @GetMapping("/message/{messageId}/feedbacks")
    public ApiResponse<List<AiFeedbackLog>> listMessageFeedbacks(@PathVariable Long messageId) {
        return ApiResponse.ok(aiLogService.listMessageFeedbacks(messageId));
    }

    @GetMapping("/user/{userId}/feedbacks")
    public ApiResponse<List<AiFeedbackLog>> listUserFeedbacks(@PathVariable Long userId) {
        return ApiResponse.ok(aiLogService.listUserFeedbacks(userId));
    }
}
