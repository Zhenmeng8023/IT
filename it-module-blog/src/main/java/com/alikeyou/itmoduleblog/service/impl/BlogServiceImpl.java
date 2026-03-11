package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.Project;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo) {
        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());
        blog.setTags(request.getTags());

        // 使用 UserInfo 实体关联获取作者信息
        UserInfo author = userRepository.findById(authorInfo.getId())
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + authorInfo.getId()));
        blog.setAuthor(author);

        // 处理项目关联
        if (request.getProjectId() != null) {
            Project project = new Project();
            project.setId(request.getProjectId());
            blog.setProject(project);
        }

        // 设置默认值
        blog.setStatus("draft");
        blog.setIsMarked(false);
        blog.setViewCount(0);
        blog.setLikeCount(0);
        blog.setCollectCount(0);
        blog.setDownloadCount(0);
        blog.setCreatedAt(Instant.now());
        blog.setUpdatedAt(Instant.now());

        return blogRepository.save(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Blog> getBlogById(Long id) {
        return blogRepository.findWithAssociationsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

    @Override
    @Transactional
    public Optional<Blog> updateBlog(Long id, BlogUpdateRequest request) {
        return blogRepository.findById(id).map(blog -> {
            if (request.getTitle() != null) {
                blog.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                blog.setContent(request.getContent());
            }
            if (request.getCoverImageUrl() != null) {
                blog.setCoverImageUrl(request.getCoverImageUrl());
            }
            if (request.getTags() != null) {
                blog.setTags(request.getTags());
            }
            if (request.getProjectId() != null) {
                Project project = new Project();
                project.setId(request.getProjectId());
                blog.setProject(project);
            }
            if (request.getStatus() != null) {
                blog.setStatus(request.getStatus());
                // 如果状态变为发布，设置发布时间
                if ("published".equals(request.getStatus()) && blog.getPublishTime() == null) {
                    blog.setPublishTime(Instant.now());
                }
            }
            if (request.getIsMarked() != null) {
                blog.setIsMarked(request.getIsMarked());
            }
            if (request.getPublishTime() != null) {
                blog.setPublishTime(request.getPublishTime());
            }
            blog.setUpdatedAt(Instant.now());
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional
    public void deleteBlog(Long id) {
        blogRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        blogRepository.incrementViewCount(id);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        blogRepository.incrementLikeCount(id);
    }

    @Override
    @Transactional
    public void incrementCollectCount(Long id) {
        blogRepository.incrementCollectCount(id);
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        blogRepository.incrementDownloadCount(id);
    }

    // 转换方法：将 Blog 实体转换为 BlogResponse
    @Override
    public BlogResponse convertToResponse(Blog blog) {
        if (blog == null) return null;

        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setCoverImageUrl(blog.getCoverImageUrl());
        response.setTags(blog.getTags());
        response.setStatus(blog.getStatus());
        response.setIsMarked(blog.getIsMarked());
        response.setPublishTime(blog.getPublishTime());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setViewCount(blog.getViewCount());
        response.setLikeCount(blog.getLikeCount());
        response.setCollectCount(blog.getCollectCount());
        response.setDownloadCount(blog.getDownloadCount());

        // 转换作者信息 - 使用 UserInfo 实体
        if (blog.getAuthor() != null) {
            BlogResponse.AuthorInfo authorInfo = new BlogResponse.AuthorInfo();
            authorInfo.setId(blog.getAuthor().getId());
            authorInfo.setUsername(blog.getAuthor().getUsername() != null ? blog.getAuthor().getUsername() : "未知用户");
            authorInfo.setAvatar(blog.getAuthor().getAvatarUrl());
            // UserInfo 没有 displayName 字段，使用 username 代替
            authorInfo.setDisplayName(blog.getAuthor().getUsername());
            // UserInfo 有 email 字段，直接设置
            authorInfo.setEmail(blog.getAuthor().getEmail());
            response.setAuthor(authorInfo);
        }

        // 转换项目信息
        if (blog.getProject() != null) {
            BlogResponse.ProjectInfo projectInfo = new BlogResponse.ProjectInfo();
            projectInfo.setId(blog.getProject().getId());
            try {
                projectInfo.setName(blog.getProject().getName());
            } catch (Exception e) {
                projectInfo.setName("未知项目");
            }
            response.setProject(projectInfo);
        }

        return response;
    }

    // 转换方法：将 Blog 列表转换为 BlogResponse 列表
    @Override
    public List<BlogResponse> convertToResponseList(List<Blog> blogs) {
        return blogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> searchBlogs(String keyword) {
        return blogRepository.searchBlogs(keyword);
    }

    @Override
    public List<Blog> findByAuthorId(Long authorId) {
        return blogRepository.findByAuthorId(authorId);
    }

    /**
     * 按标签搜索
     */
    @Override
    public List<Blog> searchBlogsByTag(String keyword) {
        return blogRepository.searchBlogsByTag(keyword);
    }

    /**
     * 按作者名搜索
     */
    @Override
    public List<Blog> searchBlogsByAuthor(String keyword) {
        return blogRepository.searchBlogsByAuthor(keyword);
    }
}
