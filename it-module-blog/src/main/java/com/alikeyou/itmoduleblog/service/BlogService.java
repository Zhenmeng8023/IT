package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import java.util.List;
import java.util.Optional;

public interface BlogService {

    Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo);

    Optional<Blog> getBlogById(Long id);

    List<Blog> getAllBlogs();

    Optional<Blog> updateBlog(Long id, BlogUpdateRequest request);

    void deleteBlog(Long id);

    void incrementViewCount(Long id);

    void incrementLikeCount(Long id);

    void incrementCollectCount(Long id);

    void incrementDownloadCount(Long id);

    BlogResponse convertToResponse(Blog blog);

    List<BlogResponse> convertToResponseList(List<Blog> blogs);

    List<Blog> searchBlogs(String keyword);

    List<Blog> findByAuthorId(Long authorId);
    List<Blog> searchBlogsByTag(String keyword);
    List<Blog> searchBlogsByAuthor(String keyword);
    List<Blog> getDraftBlogs();
}