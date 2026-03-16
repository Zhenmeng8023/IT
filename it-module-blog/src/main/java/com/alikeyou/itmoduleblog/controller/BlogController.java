package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.service.TagService;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客控制器
 * 处理与博客相关的 HTTP 请求，包括博客的创建、查询、更新、删除等操作
 */
@Tag(name = "博客管理", description = "博客文章的增删改查及互动操作")
@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserRepository userRepository;


    /**
     * 创建新博客
     * POST /api/blog
     *
     * @param request 博客创建请求参数，包含标题、内容、封面图等
     * @return 创建成功后的博客响应信息，HTTP 状态码 201
     */
    @Operation(summary = "创建新博客", description = "创建一篇新的博客文章，需要提供标题、内容等基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功创建博客",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @Parameter(description = "博客创建请求对象", required = true)
            @RequestBody BlogCreateRequest request) {
        AuthorInfo authorInfo = getCurrentUserInfo();

        var createdBlog = blogService.createBlog(request, authorInfo);
        BlogResponse response = blogService.convertToResponse(createdBlog);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 查询博客详情
     * GET /api/blog/{id}
     *
     * @param id 博客 ID
     * @return 博客详情信息，若不存在则返回 404
     */
    @Operation(summary = "查询博客详情", description = "根据博客 ID 获取单篇博客的详细信息，同时增加浏览次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客详情",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogResponse.class))),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id) {
        return blogService.getBlogById(id)
                .map(blog -> {
                    blogService.incrementViewCount(id);
                    return ResponseEntity.ok(blogService.convertToResponse(blog));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 查询所有博客列表
     * GET /api/blog
     *
     * @return 博客列表信息
     */
    @Operation(summary = "查询所有博客", description = "获取所有博客文章的列表（不分页）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        var blogs = blogService.getAllBlogs();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }


    /**
     * 更新博客信息
     * PUT /api/blog/{id}
     *
     * @param id 博客 ID
     * @param request 博客更新请求参数
     * @return 更新后的博客信息，若不存在则返回 404
     */
    @Operation(summary = "更新博客信息", description = "根据博客 ID 更新博客的详细信息，支持部分字段更新")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新博客信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogResponse.class))),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "博客更新请求对象", required = true)
            @RequestBody BlogUpdateRequest request) {
        return blogService.updateBlog(id, request)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 删除博客
     * DELETE /api/blog/{id}
     *
     * @param id 博客 ID
     * @return 删除成功返回 204
     */
    @Operation(summary = "删除博客", description = "根据博客 ID 删除指定的博客文章，删除后无法恢复")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除博客",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }


//    /**
//     * 收藏博客
//     * POST /api/blog/{id}/collect
//     *
//     * @param id 博客 ID
//     * @return 操作成功返回 200
//     */
//    @Operation(summary = "收藏博客", description = "为指定的博客文章增加一个收藏计数")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "收藏成功",
//                    content = @Content),
//            @ApiResponse(responseCode = "404", description = "博客不存在",
//                    content = @Content)
//    })
//    @PostMapping("/{id}/collect")
//    public ResponseEntity<Void> collectBlog(
//            @Parameter(description = "博客 ID", required = true, example = "1")
//            @PathVariable Long id) {
//        blogService.incrementCollectCount(id);
//        return ResponseEntity.ok().build();
//    }

    /**
     * 下载博客
     * POST /api/blog/{id}/download
     *
     * @param id 博客 ID
     * @return 操作成功返回 200
     */
    @Operation(summary = "下载博客", description = "记录博客的下载次数，用于统计")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "下载成功",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content)
    })
    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id) {
        blogService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索博客（标题和内容）
     * GET /api/blog/search?keyword=Java
     *
     * @param keyword 搜索关键词
     * @return 匹配的博客列表
     */
    @Operation(summary = "搜索博客", description = "根据关键词搜索博客标题或内容中包含该关键词的已发布博客")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功搜索博客",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/search")
    public ResponseEntity<List<BlogResponse>> searchBlogs(
            @Parameter(description = "搜索关键词", required = true, example = "Java")
            @RequestParam String keyword) {
        var blogs = blogService.searchBlogs(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 根据作者 ID 查询博客列表
     * GET /api/blog/author/{authorId}
     *
     * @param authorId 作者 ID
     * @return 该作者发布的博客列表
     */
    @Operation(summary = "按作者查询博客", description = "根据作者 ID 查询该作者发布的所有博客文章")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BlogResponse>> getBlogsByAuthorId(
            @Parameter(description = "作者 ID", required = true, example = "1")
            @PathVariable Long authorId) {
        var blogs = blogService.findByAuthorId(authorId);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 按标签搜索博客
     * GET /api/blog/search/tag?keyword=Java
     *
     * @param keyword 标签关键词
     * @return 匹配的博客列表
     */
    @Operation(summary = "按标签搜索博客", description = "根据标签关键词搜索包含该标签的已发布博客")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功搜索博客",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/search/tag")
    public ResponseEntity<List<BlogResponse>> searchBlogsByTag(
            @Parameter(description = "标签关键词", required = true, example = "Java")
            @RequestParam String keyword) {
        var blogs = blogService.searchBlogsByTag(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 按作者用户名搜索博客
     * GET /api/blog/search/author?keyword=Alice
     *
     * @param keyword 作者用户名关键词
     * @return 匹配的博客列表
     */
    @Operation(summary = "按作者名搜索博客", description = "根据作者用户名关键词搜索该作者发布的已发布博客")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功搜索博客",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/search/author")
    public ResponseEntity<List<BlogResponse>> searchBlogsByAuthor(
            @Parameter(description = "作者用户名关键词", required = true, example = "Alice")
            @RequestParam String keyword) {
        var blogs = blogService.searchBlogsByAuthor(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 获取草稿博客列表
     * GET /api/blog/drafts
     *
     * @return 草稿博客列表
     */
    @Operation(summary = "获取草稿博客列表", description = "获取当前登录用户的所有状态为草稿的博客文章，按更新时间降序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取草稿列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class)))),
            @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/draft")
    public ResponseEntity<List<BlogResponse>> getDraftBlogs() {
        // 获取当前登录用户信息
        AuthorInfo authorInfo = getCurrentUserInfo();

        // 查询当前用户的草稿博客
        var blogs = blogService.findDraftBlogsByAuthorId(authorInfo.getId());
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }


    /**
     * 按热度排序获取博客列表
     * GET /api/blog/hot
     *
     * @return 按热度排序的博客列表
     */
    @Operation(summary = "按热度排序获取博客", description = "根据综合热度（浏览量、点赞数、收藏数、下载数的加权计算）获取已发布博客列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/hot")
    public ResponseEntity<List<BlogResponse>> getHotBlogs() {
        var blogs = blogService.getBlogsByHotness();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 按时间排序获取博客列表（最新在前）
     * GET /api/blog/time/newest
     *
     * @return 按时间倒序的博客列表
     */
    @Operation(summary = "按时间排序获取博客（最新）", description = "根据发布时间倒序获取已发布博客列表，最新的在最前面")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/time/newest")
    public ResponseEntity<List<BlogResponse>> getNewestBlogs() {
        var blogs = blogService.getBlogsByTimeDesc();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 按时间排序获取博客列表（最旧在前）
     * GET /api/blog/time/oldest
     *
     * @return 按时间正序的博客列表
     */
    @Operation(summary = "按时间排序获取博客（最旧）", description = "根据发布时间正序获取已发布博客列表，最旧的在最前面")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/time/oldest")
    public ResponseEntity<List<BlogResponse>> getOldestBlogs() {
        var blogs = blogService.getBlogsByTimeAsc();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 下架博客
     * PUT /api/blog/{id}/reject
     *
     * @param id 博客 ID
     * @return 下架后的博客信息
     */
    @Operation(summary = "下架博客", description = "将指定的博客状态设置为已驳回（rejected），通常用于内容审核不通过")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功下架博客",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogResponse.class))),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content)
    })
    @PutMapping("/{id}/reject")
    public ResponseEntity<BlogResponse> rejectBlog(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id) {
        return blogService.rejectBlog(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 重新发布已下架的博客
     * PUT /api/blog/{id}/republish
     *
     * @param id 博客 ID
     * @return 重新发布后的博客信息
     */
    @Operation(summary = "重新发布博客", description = "将已下架（rejected）的博客重新设置为已发布（published）状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功重新发布博客",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogResponse.class))),
            @ApiResponse(responseCode = "404", description = "博客不存在",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "博客状态不是已下架，无法重新发布",
                    content = @Content)
    })
    @PutMapping("/{id}/republish")
    public ResponseEntity<BlogResponse> republishBlog(
            @Parameter(description = "博客 ID", required = true, example = "1")
            @PathVariable Long id) {
        return blogService.republishBlog(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 获取已下架的博客列表
     * GET /api/blog/rejected
     *
     * @return 已下架的博客列表
     */
    @Operation(summary = "获取已下架博客列表", description = "获取所有状态为已驳回（rejected）的博客文章，按更新时间降序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取已下架博客列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = BlogResponse.class))))
    })
    @GetMapping("/rejected")
    public ResponseEntity<List<BlogResponse>> getRejectedBlogs() {
        var blogs = blogService.getRejectedBlogs();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 从 LoginConstant 获取当前登录用户的完整信息
     *
     * @return 包含完整用户信息的 AuthorInfo 对象
     * @throws IllegalStateException 当用户未登录或用户不存在时抛出
     */
    private AuthorInfo getCurrentUserInfo() {
        try {
            Long userId = LoginConstant.getUserId();

            if (userId == null) {
                String username = LoginConstant.getUsername();
                if (username != null && !username.isEmpty()) {
                    UserInfo user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new IllegalStateException("用户不存在：" + username));
                    return convertToAuthorInfo(user);
                } else {
                    throw new IllegalStateException("用户未登录");
                }
            }

            UserInfo user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("用户不存在，ID: " + userId));
            return convertToAuthorInfo(user);

        } catch (Exception e) {
            throw new IllegalStateException("获取当前用户信息失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将 UserInfo 实体转换为 AuthorInfo DTO
     *
     * @param userInfo 用户实体对象
     * @return 作者信息 DTO 对象
     */
    private AuthorInfo convertToAuthorInfo(UserInfo userInfo) {
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setId(userInfo.getId());
        authorInfo.setUsername(userInfo.getUsername());
        authorInfo.setNickname(userInfo.getNickname());
        authorInfo.setAvatar(userInfo.getAvatarUrl());
        authorInfo.setEmail(userInfo.getEmail());
        authorInfo.setDisplayName(
                userInfo.getNickname() != null ?
                        userInfo.getNickname() :
                        userInfo.getUsername()
        );
        return authorInfo;
    }
}
