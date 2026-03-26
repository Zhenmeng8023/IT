package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/models")
@RequiredArgsConstructor
public class AiModelAdminController {

    private final AiModelService aiModelService;

    @GetMapping
    public ApiResponse<Page<AiModel>> page(Pageable pageable) {
        return ApiResponse.ok(aiModelService.page(pageable));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<AiModel>> listEnabled() {
        return ApiResponse.ok(aiModelService.listEnabled());
    }

    @GetMapping("/active")
    public ApiResponse<AiModel> active() {
        return ApiResponse.ok(aiModelService.getActiveModel());
    }

    @GetMapping("/{id}")
    public ApiResponse<AiModel> get(@PathVariable Long id) {
        return ApiResponse.ok(aiModelService.getById(id));
    }

    @PostMapping
    public ApiResponse<AiModel> save(@RequestBody AiModel entity) {
        return ApiResponse.ok("保存成功", aiModelService.save(entity));
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<AiModel> enable(@PathVariable Long id) {
        return ApiResponse.ok("启用成功", aiModelService.enable(id));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        aiModelService.disable(id);
        return ApiResponse.ok("停用成功", null);
    }
}
