package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleuser.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// 假设这里有用户认证，获取当前登录用户
// import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogCreateRequest request) {
        // 假设这里获取当前登录用户
        // 实际项目中应该从认证上下文获取
        UserInfo author = new UserInfo();
        author.setId(1L); // 临时设置，实际应该从认证信息获取

        Blog createdBlog = blogService.createBlog(request, author);
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
        List<Blog> blogs = blogService.getAllBlogs();
        List<BlogResponse> responses = blogService.convertToResponseList(blogs);
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
}