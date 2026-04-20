package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogSearchRequest;
import com.alikeyou.itmoduleblog.dto.BlogSearchResult;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmodulecommon.entity.Report;

import java.util.List;
import java.util.Optional;

public interface BlogService {

    Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo);

    Optional<Blog> getBlogById(Long id);

    Optional<Blog> getBlogByIdVisible(Long id, Long viewerId, boolean adminReviewer);

    List<Blog> getAllBlogs();

    Optional<Blog> updateBlog(Long id, BlogUpdateRequest request);

    Optional<Blog> updateBlog(Long id, BlogUpdateRequest request, Long operatorId, boolean adminReviewer);

    void deleteBlog(Long id);

    void deleteBlog(Long id, Long operatorId, boolean adminReviewer);

    void incrementViewCount(Long id);

    void incrementCollectCount(Long id);

    void incrementDownloadCount(Long id);

    void incrementLikeCount(Long id);

    BlogResponse convertToResponse(Blog blog);

    BlogResponse convertToSecureResponse(Blog blog, Long viewerId);

    BlogResponse convertToSecureResponse(Blog blog, Long viewerId, boolean adminReviewer);

    BlogResponse convertToSecurePreviewResponse(Blog blog, Long viewerId, boolean adminReviewer);

    List<BlogResponse> convertToResponseList(List<Blog> blogs);

    List<BlogResponse> convertToSecureResponseList(List<Blog> blogs, Long viewerId, boolean adminReviewer);

    List<BlogResponse> convertToSecurePreviewResponseList(List<Blog> blogs, Long viewerId, boolean adminReviewer);

    List<Blog> searchBlogs(String keyword);

    List<Blog> searchBlogs(String keyword, Long viewerId, boolean adminReviewer);

    BlogSearchResult searchBlogs(BlogSearchRequest request, Long viewerId, boolean adminReviewer);

    List<Blog> findByAuthorId(Long authorId);

    List<Blog> findByAuthorIdVisible(Long authorId, Long viewerId, boolean adminReviewer);

    List<Blog> searchBlogsByTag(String keyword);

    List<Blog> searchBlogsByTag(String keyword, Long viewerId, boolean adminReviewer);

    List<Blog> searchBlogsByAuthor(String keyword);

    List<Blog> searchBlogsByAuthor(String keyword, Long viewerId, boolean adminReviewer);

    List<Blog> findDraftBlogsByAuthorId(Long authorId);

    List<Blog> getBlogsByHotness();

    List<Blog> getBlogsByTimeDesc();

    List<Blog> getBlogsByTimeAsc();

    List<Blog> getReportedBlogs();

    Optional<Blog> rejectBlog(Long id, String reason, Long operatorId);

    Optional<Blog> republishBlog(Long id);

    List<Blog> getRejectedBlogs();

    List<Blog> getRejectedBlogs(Long viewerId, boolean adminReviewer);

    org.springframework.data.domain.Page<Blog> getPendingBlogs(org.springframework.data.domain.Pageable pageable);

    Optional<Blog> approveBlog(Long id, Long operatorId);

    void batchReviewBlogs(java.util.List<Long> blogIds, String status, String reason, Long operatorId);

    Report reportBlog(Long blogId, Long reporterId, String reason);

    java.util.List<Report> getReportsByBlogId(Long blogId);
}
