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
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmoduleblog.exception.BlogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Override
    @Transactional
    public Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo) {
        if (request == null) {
            throw new BlogException("创建博客的请求参数不能为空");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BlogException("博客标题不能为空");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BlogException("博客内容不能为空");
        }

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            if (tags.size() != request.getTagIds().size()) {
                Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                Set<Long> invalidTagIds = request.getTagIds().stream()
                        .filter(id -> !validTagIds.contains(id))
                        .collect(Collectors.toSet());
                throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
            }
            Map<String, Object> tagsMap = tags.stream()
                    .collect(Collectors.toMap(
                            tag -> tag.getId().toString(),
                            tag -> tag.getName()
                    ));
            blog.setTags(tagsMap);
        }

        UserInfo author = userRepository.findById(authorInfo.getId())
                .orElseThrow(() -> new BlogException("用户不存在，ID: " + authorInfo.getId()));
        blog.setAuthor(author);

        if (request.getProjectId() != null) {
            Project project = new Project();
            project.setId(request.getProjectId());
            blog.setProject(project);
        }

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
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return blogRepository.findWithAssociationsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getAllBlogs() {
        return blogRepository.findPublishedBlogs();
    }

    @Override
    @Transactional
    public Optional<Blog> updateBlog(Long blogId, BlogUpdateRequest request) {
        if (blogId == null) {
            throw new BlogException("博客 ID 不能为空");
        }

        if (request == null) {
            throw new BlogException("更新请求参数不能为空");
        }

        return blogRepository.findById(blogId).map(blog -> {
            if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
                throw new BlogException("博客标题不能为空");
            }
            if (request.getContent() != null && request.getContent().trim().isEmpty()) {
                throw new BlogException("博客内容不能为空");
            }

            if (request.getTitle() != null) {
                blog.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                blog.setContent(request.getContent());
            }
            if (request.getCoverImageUrl() != null) {
                blog.setCoverImageUrl(request.getCoverImageUrl());
            }
            if (request.getTagIds() != null) {
                List<Tag> tags = tagRepository.findAllById(request.getTagIds());
                if (tags.size() != request.getTagIds().size()) {
                    Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                    Set<Long> invalidTagIds = request.getTagIds().stream()
                            .filter(id -> !validTagIds.contains(id))
                            .collect(Collectors.toSet());
                    throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
                }
                Map<String, Object> tagsMap = tags.stream()
                        .collect(Collectors.toMap(
                                tag -> tag.getId().toString(),
                                tag -> tag.getName()
                        ));
                blog.setTags(tagsMap);
            }
            if (request.getProjectId() != null) {
                Project project = new Project();
                project.setId(request.getProjectId());
                blog.setProject(project);
            }
            if (request.getStatus() != null) {
                blog.setStatus(request.getStatus());
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
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }

        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogException("博客不存在，ID: " + id));
        blogRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementViewCount(id);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementLikeCount(id);
    }

    @Override
    @Transactional
    public void incrementCollectCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementCollectCount(id);
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementDownloadCount(id);
    }

    @Override
    public BlogResponse convertToResponse(Blog blog) {
        if (blog == null) {
            return null;
        }

        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setCoverImageUrl(blog.getCoverImageUrl());
        if (blog.getTags() != null) {
            response.setTags(blog.getTags().values().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        } else {
            response.setTags(null);
        }
        response.setStatus(blog.getStatus());
        response.setIsMarked(blog.getIsMarked());
        response.setPublishTime(blog.getPublishTime());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setViewCount(blog.getViewCount());
        response.setLikeCount(blog.getLikeCount());
        response.setCollectCount(blog.getCollectCount());
        response.setDownloadCount(blog.getDownloadCount());

        if (blog.getAuthor() != null) {
            BlogResponse.AuthorInfo authorInfo = new BlogResponse.AuthorInfo();
            authorInfo.setId(blog.getAuthor().getId());
            authorInfo.setUsername(blog.getAuthor().getUsername() != null ? blog.getAuthor().getUsername() : "未知用户");
            authorInfo.setNickname(blog.getAuthor().getNickname());  // 设置昵称
            authorInfo.setAvatar(blog.getAuthor().getAvatarUrl());
            // 优先使用昵称，如果昵称为空则使用用户名作为显示名称
            authorInfo.setDisplayName(blog.getAuthor().getNickname() != null ?
                    blog.getAuthor().getNickname() :
                    blog.getAuthor().getUsername());
            authorInfo.setEmail(blog.getAuthor().getEmail());
            response.setAuthor(authorInfo);
        }

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

    @Override
    public List<BlogResponse> convertToResponseList(List<Blog> blogs) {
        return blogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> searchBlogs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }
        return blogRepository.searchBlogs(keyword);
    }

    @Override
    public List<Blog> findByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new BlogException("作者 ID 不能为空");
        }
        return blogRepository.findByAuthorId(authorId);
    }

    @Override
    public List<Blog> searchBlogsByTag(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }
        return blogRepository.searchBlogsByTag(keyword);
    }

    @Override
    public List<Blog> searchBlogsByAuthor(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }
        return blogRepository.searchBlogsByAuthor(keyword);
    }

    @Override
    public List<Blog> getDraftBlogs() {
        return blogRepository.findDraftBlogs();
    }


    @Override
    public List<Blog> getBlogsByHotness() {
        return blogRepository.findByHotness();
    }

    @Override
    public List<Blog> getBlogsByTimeDesc() {
        return blogRepository.findByTimeDesc();
    }

    @Override
    public List<Blog> getBlogsByTimeAsc() {
        return blogRepository.findByTimeAsc();
    }

    @Override
    @Transactional
    public Optional<Blog> rejectBlog(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }

        return blogRepository.findById(id).map(blog -> {
            blog.setStatus("rejected");
            blog.setUpdatedAt(Instant.now());
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional
    public Optional<Blog> republishBlog(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }

        return blogRepository.findById(id).map(blog -> {
            if (!"rejected".equals(blog.getStatus())) {
                throw new BlogException("只有已下架的博客才能重新发布，当前博客状态：" + blog.getStatus());
            }
            blog.setStatus("published");
            blog.setUpdatedAt(Instant.now());
            if (blog.getPublishTime() == null) {
                blog.setPublishTime(Instant.now());
            }
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getRejectedBlogs() {
        return blogRepository.findRejectedBlogs();
    }
}
