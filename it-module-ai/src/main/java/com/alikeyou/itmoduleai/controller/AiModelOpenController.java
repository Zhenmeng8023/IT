package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.model.AiModelAdminVO;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
public class AiModelOpenController {

    private final AiModelService aiModelService;

    @GetMapping("/enabled")
    public ApiResponse<List<AiModelAdminVO>> listEnabled() {
        Long activeId = getActiveIdSafely();
        try {
            List<AiModelAdminVO> data = aiModelService.listEnabled()
                    .stream()
                    .map(item -> AiModelAdminVO.from(item, activeId))
                    .toList();
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.ok(Collections.emptyList());
        }
    }

    @GetMapping("/active")
    public ApiResponse<AiModelAdminVO> active() {
        try {
            AiModel active = aiModelService.getActiveModel();
            return ApiResponse.ok(AiModelAdminVO.from(active, active == null ? null : active.getId()));
        } catch (Exception e) {
            return ApiResponse.ok((AiModelAdminVO) null);
        }
    }

    private Long getActiveIdSafely() {
        try {
            AiModel active = aiModelService.getActiveModel();
            return active == null ? null : active.getId();
        } catch (Exception e) {
            return null;
        }
    }
}
