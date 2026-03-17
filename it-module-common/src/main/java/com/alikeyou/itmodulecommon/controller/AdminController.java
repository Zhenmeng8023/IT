package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
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
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private TagService tagService;

    @Autowired
    private UserInfoService userInfoService;

    // 分页获取标签列表
    @Operation(summary = "分页获取标签列表", description = "(后台) 分页查询所有标签")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取标签列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/tags/page")
    public ResponseEntity<Page<Tag>> getTagsPage(
            @Parameter(description = "页码", required = true)
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", required = true)
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Tag> tagsPage = tagService.getTagsPage(pageable);
        return ResponseEntity.ok(tagsPage);
    }

    // 创建新标签
    @Operation(summary = "创建新标签", description = "(后台) 创建一个新的标签")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功创建标签",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Tag.class)))
    })
    @PostMapping("/tags")
    public ResponseEntity<Tag> createTag(
            @Parameter(description = "标签信息", required = true)
            @RequestBody Tag tag) {
        Tag createdTag = tagService.createTag(tag);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
    }

    // 更新标签信息
    @Operation(summary = "更新标签信息", description = "(后台) 更新一个标签的信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新标签",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Tag.class))),
            @ApiResponse(responseCode = "404", description = "标签不存在")
    })
    @PutMapping("/tags/{id}")
    public ResponseEntity<Tag> updateTag(
            @Parameter(description = "标签ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "标签信息", required = true)
            @RequestBody Tag tag) {
        Tag updatedTag = tagService.updateTag(id, tag);
        if (updatedTag != null) {
            return ResponseEntity.ok(updatedTag);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // 删除标签
    @Operation(summary = "删除标签", description = "(后台) 删除一个标签")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除标签")
    })
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(
            @Parameter(description = "标签ID", required = true)
            @PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 分页获取用户列表
    @Operation(summary = "分页获取用户列表", description = "(后台) 分页查询所有用户，支持按条件筛选")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/users/page")
    public ResponseEntity<Page<UserInfo>> getUsersPage(
            @Parameter(description = "页码", required = true)
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", required = true)
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserInfo> usersPage = userInfoService.getUsersPage(pageable);
        return ResponseEntity.ok(usersPage);
    }

    // 批量删除用户
    @Operation(summary = "批量删除用户", description = "(后台) 根据用户 ID 列表批量删除用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除用户")
    })
    @DeleteMapping("/users/batch")
    public ResponseEntity<Void> batchDeleteUsers(
            @Parameter(description = "用户ID列表", required = true)
            @RequestBody List<Long> userIds) {
        userInfoService.batchDeleteUsers(userIds);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}