package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
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

    @PostMapping  // 原来是 @PostMapping("/blogs")
    public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogCreateRequest request) {
        // 假设这里获取当前登录用户
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setId(2L); // 临时设置，实际应该从认证信息获取
        authorInfo.setUsername("当前用户"); // 临时设置
        authorInfo.setAvatar(null); // 临时设置

        var createdBlog = blogService.createBlog(request, authorInfo);
        BlogResponse response = blogService.convertToResponse(createdBlog);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id) {
        return blogService.getBlogById(id)
                .map(blog -> {
                    // 增加浏览次数
                    blogService.incrementViewCount(id);
                    return ResponseEntity.ok(blogService.convertToResponse(blog));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        var blogs = blogService.getAllBlogs();
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(@PathVariable Long id, @RequestBody BlogUpdateRequest request) {
        return blogService.updateBlog(id, request)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeBlog(@PathVariable Long id) {
        blogService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<Void> collectBlog(@PathVariable Long id) {
        blogService.incrementCollectCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(@PathVariable Long id) {
        blogService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索博客
     * GET /api/blogs/search?keyword=Java
     */
    @GetMapping("/search")
    public ResponseEntity<List<BlogResponse>> searchBlogs(@RequestParam String keyword) {
        var blogs = blogService.searchBlogs(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 根据作者 ID 查询博客
     * GET /api/blogs/author/{authorId}
     */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BlogResponse>> getBlogsByAuthorId(@PathVariable Long authorId) {
        var blogs = blogService.findByAuthorId(authorId);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 搜索博客（标签）
     * GET /api/blogs/search/tag?keyword=Java
     */
    @GetMapping("/search/tag")
    public ResponseEntity<List<BlogResponse>> searchBlogsByTag(@RequestParam String keyword) {
        var blogs = blogService.searchBlogsByTag(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }

    /**
     * 搜索博客（作者）
     * GET /api/blogs/search/author?keyword=Alice
     */
    @GetMapping("/search/author")
    public ResponseEntity<List<BlogResponse>> searchBlogsByAuthor(@RequestParam String keyword) {
        var blogs = blogService.searchBlogsByAuthor(keyword);
        var responses = blogService.convertToResponseList(blogs);
        return ResponseEntity.ok(responses);
    }
}
