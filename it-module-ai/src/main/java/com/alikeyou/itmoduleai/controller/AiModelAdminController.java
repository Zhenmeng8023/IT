package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.model.AiModelAdminVO;
import com.alikeyou.itmoduleai.dto.model.AiModelConnectionTestResult;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/models")
@RequiredArgsConstructor
public class AiModelAdminController {

    private final AiModelService aiModelService;

    @GetMapping
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<Page<AiModelAdminVO>> page(Pageable pageable) {
        Long activeId = getActiveIdSafely();
        return ApiResponse.ok(aiModelService.page(pageable).map(item -> AiModelAdminVO.from(item, activeId)));
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<List<AiModelAdminVO>> listEnabled() {
        Long activeId = getActiveIdSafely();
        return ApiResponse.ok(aiModelService.listEnabled().stream().map(item -> AiModelAdminVO.from(item, activeId)).toList());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelAdminVO> active() {
        AiModel active = aiModelService.getActiveModel();
        return ApiResponse.ok(AiModelAdminVO.from(active, active.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelAdminVO> get(@PathVariable Long id) {
        return ApiResponse.ok(AiModelAdminVO.from(aiModelService.getById(id), getActiveIdSafely()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelAdminVO> save(@RequestBody AiModel entity) {
        AiModel saved = aiModelService.save(entity);
        return ApiResponse.ok("保存成功", AiModelAdminVO.from(saved, getActiveIdSafely()));
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelAdminVO> enable(@PathVariable Long id) {
        AiModel saved = aiModelService.enable(id);
        return ApiResponse.ok("启用成功", AiModelAdminVO.from(saved, getActiveIdSafely()));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelAdminVO> activate(@PathVariable Long id) {
        AiModel saved = aiModelService.activate(id);
        return ApiResponse.ok("设置当前模型成功", AiModelAdminVO.from(saved, saved.getId()));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        aiModelService.disable(id);
        return ApiResponse.ok("停用成功", null);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('view:ai:model-admin')")
    public ApiResponse<AiModelConnectionTestResult> test(@PathVariable Long id) {
        return ApiResponse.ok("测试完成", aiModelService.testConnection(id));
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
