package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleuser.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 假设这里有用户认证，获取当前登录用户
// import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @PostMapping
    public ResponseEntity<Blog> createBlog(@RequestBody BlogCreateRequest request) {
        // 假设这里获取当前登录用户
        // 实际项目中应该从认证上下文获取
        UserInfo author = new UserInfo();
        author.setId(1L); // 临时设置，实际应该从认证信息获取

        Blog createdBlog = blogService.createBlog(request, author);
        return new ResponseEntity<>(createdBlog, HttpStatus.CREATED);
    }
}