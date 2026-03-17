package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlogService {

    Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo);

    Optional<Blog> getBlogById(Long id);

    List<Blog> getAllBlogs();

    Optional<Blog> updateBlog(Long id, BlogUpdateRequest request);

    void deleteBlog(Long id);

    void incrementViewCount(Long id);

    void incrementCollectCount(Long id);

    void incrementDownloadCount(Long id);

    void incrementLikeCount(Long id);

    BlogResponse convertToResponse(Blog blog);

    List<BlogResponse> convertToResponseList(List<Blog> blogs);

    List<Blog> searchBlogs(String keyword);

    List<Blog> findByAuthorId(Long authorId);
    List<Blog> searchBlogsByTag(String keyword);
    List<Blog> searchBlogsByAuthor(String keyword);
    List<Blog> findDraftBlogsByAuthorId(Long authorId);
    /**
     * 按热度排序获取博客列表
     */
    List<Blog> getBlogsByHotness();

    /**
     * 按时间排序获取博客列表（最新在前）
     */
    List<Blog> getBlogsByTimeDesc();

    /**
     * 按时间排序获取博客列表（最旧在前）
     */
    List<Blog> getBlogsByTimeAsc();

    Optional<Blog> rejectBlog(Long id);

    /**
     * 重新发布已下架的博客（将状态从 rejected 改为 published）
     */
    Optional<Blog> republishBlog(Long id);

    /**
     * 获取已下架的博客列表
     */
    List<Blog> getRejectedBlogs();

    /**
     * 分页获取待审核博客列表
     */
    org.springframework.data.domain.Page<Blog> getPendingBlogs(org.springframework.data.domain.Pageable pageable);

    /**
     * 审核博客通过
     */
    Optional<Blog> approveBlog(Long id);


    /**
     * 批量审核博客
     */
    void batchReviewBlogs(java.util.List<Long> blogIds, String status, String reason);
}