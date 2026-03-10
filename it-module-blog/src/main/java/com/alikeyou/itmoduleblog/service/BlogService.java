package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.Project;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleuser.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Transactional
    public Blog createBlog(BlogCreateRequest request, UserInfo author) {
        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());
        blog.setTags(request.getTags());
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

    @Transactional(readOnly = true)
    public Optional<Blog> getBlogById(Long id) {
        return blogRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

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

    @Transactional
    public void deleteBlog(Long id) {
        blogRepository.deleteById(id);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setViewCount(blog.getViewCount() + 1);
            blogRepository.save(blog);
        });
    }

    @Transactional
    public void incrementLikeCount(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setLikeCount(blog.getLikeCount() + 1);
            blogRepository.save(blog);
        });
    }

    @Transactional
    public void incrementCollectCount(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setCollectCount(blog.getCollectCount() + 1);
            blogRepository.save(blog);
        });
    }

    @Transactional
    public void incrementDownloadCount(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setDownloadCount(blog.getDownloadCount() + 1);
            blogRepository.save(blog);
        });
    }

    // 转换方法：将Blog实体转换为BlogResponse
    public BlogResponse convertToResponse(Blog blog) {
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

        // 转换作者信息
//        if (blog.getAuthor() != null) {
//            BlogResponse.AuthorInfo authorInfo = new BlogResponse.AuthorInfo();
//            authorInfo.setId(blog.getAuthor().getId());
//            // 假设UserInfo有username和avatar字段
//            try {
//                authorInfo.setUsername(blog.getAuthor().getUsername());
//                authorInfo.setAvatar(blog.getAuthor().getAvatar());
//            } catch (Exception e) {
//                // 如果UserInfo没有这些字段，设置默认值
//                authorInfo.setUsername("未知用户");
//                authorInfo.setAvatar(null);
//            }
//            response.setAuthor(authorInfo);
//        }

        // 转换项目信息
        if (blog.getProject() != null) {
            BlogResponse.ProjectInfo projectInfo = new BlogResponse.ProjectInfo();
            projectInfo.setId(blog.getProject().getId());
            // 假设Project有name字段
            try {
                projectInfo.setName(blog.getProject().getName());
            } catch (Exception e) {
                // 如果Project没有name字段，设置默认值
                projectInfo.setName("未知项目");
            }
            response.setProject(projectInfo);
        }

        return response;
    }

    // 转换方法：将Blog列表转换为BlogResponse列表
    public List<BlogResponse> convertToResponseList(List<Blog> blogs) {
        return blogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}