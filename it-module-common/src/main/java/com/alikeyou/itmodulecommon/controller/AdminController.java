package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.dto.AdminResetPasswordDTO;
import com.alikeyou.itmodulecommon.dto.TagRequest;
import com.alikeyou.itmodulecommon.dto.TagResponse;
import com.alikeyou.itmodulecommon.dto.UserResponseDTO;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.service.TagReferenceService;
import com.alikeyou.itmodulecommon.service.TagService;
import com.alikeyou.itmodulecommon.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private TagService tagService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired(required = false)
    private TagReferenceService tagReferenceService;

    @Operation(summary = "Page tags")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/tags/page")
    public ResponseEntity<Page<TagResponse>> getTagsPage(
            @Parameter(description = "Page number", required = true)
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", required = true)
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Tag> tagsPage = tagService.getTagsPage(pageable);
        Map<Long, Long> usageCountMap = getUsageCountMap();
        return ResponseEntity.ok(tagsPage.map(tag -> TagResponse.from(tag, usageCountMap.getOrDefault(tag.getId(), 0L))));
    }

    @Operation(summary = "Create tag")
    @PostMapping("/tags")
    public ResponseEntity<TagResponse> createTag(@RequestBody TagRequest request) {
        Tag createdTag = tagService.createTag(request);
        long useCount = getUsageCountMap().getOrDefault(createdTag.getId(), 0L);
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(createdTag, useCount));
    }

    @Operation(summary = "Update tag")
    @PutMapping("/tags/{id}")
    public ResponseEntity<TagResponse> updateTag(@PathVariable Long id, @RequestBody TagRequest request) {
        Tag updatedTag = tagService.updateTag(id, request);
        long useCount = getUsageCountMap().getOrDefault(updatedTag.getId(), 0L);
        return ResponseEntity.ok(TagResponse.from(updatedTag, useCount));
    }

    @Operation(summary = "Delete tag")
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Page users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/users/page")
    public ResponseEntity<Page<UserResponseDTO>> getUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer roleId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserInfo> usersPage = userInfoService.getUsersPage(username, email, phone, status, roleId, pageable);
        return ResponseEntity.ok(usersPage.map(UserResponseDTO::new));
    }

    @Operation(summary = "Batch delete users")
    @DeleteMapping("/users/batch")
    public ResponseEntity<Void> batchDeleteUsers(@RequestBody List<Long> userIds) {
        userInfoService.batchDeleteUsers(userIds);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Admin reset user password")
    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetUserPassword(@PathVariable Long id, @RequestBody AdminResetPasswordDTO request) {
        if (request == null || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean success = userInfoService.resetPassword(id, request.getNewPassword());
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    private Map<Long, Long> getUsageCountMap() {
        if (tagReferenceService == null) {
            return Collections.emptyMap();
        }
        return tagReferenceService.countPublishedTagUsage();
    }
}
