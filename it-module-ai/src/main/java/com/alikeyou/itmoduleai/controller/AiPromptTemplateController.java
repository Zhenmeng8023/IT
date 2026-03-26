package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.service.AiPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/prompt-templates")
@RequiredArgsConstructor
public class AiPromptTemplateController {

    private final AiPromptTemplateService aiPromptTemplateService;

    @GetMapping
    public ApiResponse<Page<AiPromptTemplate>> page(Pageable pageable) {
        return ApiResponse.ok(aiPromptTemplateService.page(pageable));
    }

    @GetMapping("/scene/{sceneCode}")
    public ApiResponse<List<AiPromptTemplate>> listByScene(@PathVariable String sceneCode) {
        return ApiResponse.ok(aiPromptTemplateService.listByScene(sceneCode));
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<AiPromptTemplate>> listByOwner(@PathVariable Long ownerId) {
        return ApiResponse.ok(aiPromptTemplateService.listByOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiPromptTemplate> get(@PathVariable Long id) {
        return ApiResponse.ok(aiPromptTemplateService.getById(id));
    }

    @PostMapping
    public ApiResponse<AiPromptTemplate> save(@RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("保存成功", aiPromptTemplateService.save(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiPromptTemplate> update(@PathVariable Long id, @RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("更新成功", aiPromptTemplateService.update(id, entity));
    }

    @PutMapping("/{id}/publish")
    public ApiResponse<AiPromptTemplate> publish(@PathVariable Long id) {
        return ApiResponse.ok("发布成功", aiPromptTemplateService.publish(id));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<AiPromptTemplate> disable(@PathVariable Long id) {
        return ApiResponse.ok("停用成功", aiPromptTemplateService.disable(id));
    }
}
