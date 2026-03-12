package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")  // 修改这里
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Operation(summary = "创建博客", description = "创建新的博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "成功创建博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @PostMapping  // 原来是 @PostMapping("/blogs")
    public ResponseEntity<BlogResponse> createBlog(
        @Parameter(description = "博客创建请求", required = true) 
        @RequestBody BlogCreateRequest request) {
        // 假设这里获取当前登录用户
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setId(2L); // 临时设置，实际应该从认证信息获取
        authorInfo.setUsername("当前用户"); // 临时设置
        authorInfo.setAvatar(null); // 临时设置

        var createdBlog = blogService.createBlog(request, authorInfo);
        BlogResponse response = blogService.convertToResponse(createdBlog);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "获取博客详情", description = "根据博客ID获取博客详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取博客详情", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class))),
        @ApiResponse(responseCode = "404", description = "博客不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id) {
        return blogService.getBlogById(id)
                .map(blog -> {
                    // 增加浏览次数
                    blogService.incrementViewCount(id);
                    return ResponseEntity.ok(blogService.convertToResponse(blog));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(summary = "获取所有博客", description = "获取所有博客列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取博客列表", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        var blogs = blogService.getAllBlogs();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "更新博客", description = "更新现有博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功更新博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class))),
        @ApiResponse(responseCode = "404", description = "博客不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id,
        @Parameter(description = "博客更新请求", required = true) 
        @RequestBody BlogUpdateRequest request) {
        return blogService.updateBlog(id, request)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(summary = "删除博客", description = "删除现有博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "成功删除博客")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "点赞博客", description = "为博客点赞")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功点赞")
    })
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeBlog(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id) {
        blogService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "收藏博客", description = "收藏博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功收藏")
    })
    @PostMapping("/{id}/collect")
    public ResponseEntity<Void> collectBlog(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id) {
        blogService.incrementCollectCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "下载博客", description = "下载博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功下载")
    })
    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(
        @Parameter(description = "博客ID", required = true) 
        @PathVariable Long id) {
        blogService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索博客
     * GET /api/blogs/search?keyword=Java
     */
    @Operation(summary = "搜索博客", description = "根据关键词搜索博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功搜索到博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<BlogResponse>> searchBlogs(
        @Parameter(description = "搜索关键词", required = true) 
        @RequestParam String keyword) {
        var blogs = blogService.searchBlogs(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 根据作者 ID 查询博客
     * GET /api/blogs/author/{authorId}
     */
    @Operation(summary = "根据作者ID获取博客", description = "根据作者ID获取博客列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取作者博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BlogResponse>> getBlogsByAuthorId(
        @Parameter(description = "作者ID", required = true) 
        @PathVariable Long authorId) {
        var blogs = blogService.findByAuthorId(authorId);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 搜索博客（标签）
     * GET /api/blogs/search/tag?keyword=Java
     */
    @Operation(summary = "根据标签搜索博客", description = "根据标签关键词搜索博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功搜索到博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @GetMapping("/search/tag")
    public ResponseEntity<List<BlogResponse>> searchBlogsByTag(
        @Parameter(description = "标签关键词", required = true) 
        @RequestParam String keyword) {
        var blogs = blogService.searchBlogsByTag(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 搜索博客（作者）
     * GET /api/blogs/search/author?keyword=Alice
     */
    @Operation(summary = "根据作者搜索博客", description = "根据作者关键词搜索博客")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功搜索到博客", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogResponse.class)))
    })
    @GetMapping("/search/author")
    public ResponseEntity<List<BlogResponse>> searchBlogsByAuthor(
        @Parameter(description = "作者关键词", required = true) 
        @RequestParam String keyword) {
        var blogs = blogService.searchBlogsByAuthor(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }
}