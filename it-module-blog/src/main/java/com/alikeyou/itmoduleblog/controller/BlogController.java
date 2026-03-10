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
        authorInfo.setId(1L); // 临时设置，实际应该从认证信息获取
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
    public ResponseEntity<Void>collectBlog(@PathVariable Long id) {
        blogService.incrementCollectCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(@PathVariable Long id) {
        blogService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }
}
