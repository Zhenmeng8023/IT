package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.service.AiPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/prompt-templates")
@RequiredArgsConstructor
public class AiPromptTemplateController {

    private final AiPromptTemplateService aiPromptTemplateService;

    @GetMapping
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<Page<AiPromptTemplate>> page(Pageable pageable) {
        return ApiResponse.ok(aiPromptTemplateService.page(pageable));
    }

    @GetMapping("/scene/{sceneCode}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiPromptTemplate>> listByScene(@PathVariable String sceneCode) {
        return ApiResponse.ok(aiPromptTemplateService.listByScene(sceneCode));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("@authorizationGuard.canAccessPromptTemplateOwner(#ownerId)")
    public ApiResponse<List<AiPromptTemplate>> listByOwner(@PathVariable Long ownerId) {
        return ApiResponse.ok(aiPromptTemplateService.listByOwner(ownerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiPromptTemplate> get(@PathVariable Long id) {
        return ApiResponse.ok(aiPromptTemplateService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> save(@RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("淇濆瓨鎴愬姛", aiPromptTemplateService.save(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> update(@PathVariable Long id, @RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("鏇存柊鎴愬姛", aiPromptTemplateService.update(id, entity));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> publish(@PathVariable Long id) {
        return ApiResponse.ok("鍙戝竷鎴愬姛", aiPromptTemplateService.publish(id));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> disable(@PathVariable Long id) {
        return ApiResponse.ok("鍋滅敤鎴愬姛", aiPromptTemplateService.disable(id));
    }
}
