package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.BlogRecommendationResult;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulerecommend.dto.BlogRecommendationSnapshot;
import com.alikeyou.itmodulerecommend.service.RecommendationResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogRecommendationServiceImplTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private BlogService blogService;
    @Mock
    private RecommendationResultService recommendationResultService;

    private BlogRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogRecommendationServiceImpl();
        ReflectionTestUtils.setField(service, "blogRepository", blogRepository);
        ReflectionTestUtils.setField(service, "blogService", blogService);
        ReflectionTestUtils.setField(service, "recommendationResultService", recommendationResultService);
    }

    @Test
    void getRecommendationsShouldRerankMergedCandidatesByCurrentBlogSimilarity() {
        Blog current = buildBlog(1L, 10L, "Java Spring Cache", "Java Spring 缓存优化", tags("1", "java", "2", "spring"));
        Blog algorithmOnly = buildBlog(2L, 20L, "Python ML", "机器学习", tags("3", "python"));
        Blog similarContext = buildBlog(3L, 30L, "Spring Cache 实战", "Java Spring 主题", tags("1", "java", "2", "spring"));

        BlogRecommendationSnapshot snapshot = new BlogRecommendationSnapshot();
        snapshot.setAlgorithmVersion("blog_hybrid_v1");
        snapshot.setGeneratedAt(Instant.parse("2026-04-20T00:00:00Z"));
        snapshot.setBlogIds(List.of(2L));

        when(blogRepository.findWithAssociationsById(1L)).thenReturn(Optional.of(current));
        when(recommendationResultService.getLatestBlogRecommendations(9L, 2, false)).thenReturn(snapshot);
        when(blogRepository.findByIdIn(List.of(2L))).thenReturn(List.of(algorithmOnly));
        when(blogRepository.findByAuthorId(10L)).thenReturn(List.of(current));
        when(blogRepository.findByHotness()).thenReturn(List.of(algorithmOnly, similarContext));
        when(blogRepository.findByTimeDesc()).thenReturn(List.of(similarContext));
        when(blogService.convertToSecurePreviewResponse(algorithmOnly, 9L, false)).thenReturn(responseOf(algorithmOnly));
        when(blogService.convertToSecurePreviewResponse(similarContext, 9L, false)).thenReturn(responseOf(similarContext));

        BlogRecommendationResult result = service.getRecommendations(1L, 9L, 2);

        assertEquals("mixed", result.getSource());
        assertEquals(2, result.getItems().size());
        assertIterableEquals(List.of(3L, 2L), result.getItems().stream().map(BlogResponse::getId).toList());
    }

    @Test
    void getRecommendationsShouldExcludeCurrentBlogFromAlgorithmAndFallbackCandidates() {
        Blog current = buildBlog(1L, 10L, "Java Spring", "Java Spring", tags("1", "java"));
        Blog other = buildBlog(4L, 40L, "Java 并发", "并发", tags("1", "java"));

        BlogRecommendationSnapshot snapshot = new BlogRecommendationSnapshot();
        snapshot.setBlogIds(List.of(1L, 4L));

        when(blogRepository.findWithAssociationsById(1L)).thenReturn(Optional.of(current));
        when(recommendationResultService.getLatestBlogRecommendations(8L, 3, false)).thenReturn(snapshot);
        when(blogRepository.findByIdIn(List.of(4L))).thenReturn(List.of(other));
        when(blogRepository.findByAuthorId(10L)).thenReturn(List.of(current));
        when(blogRepository.findByHotness()).thenReturn(List.of(current, other));
        when(blogRepository.findByTimeDesc()).thenReturn(List.of(current, other));
        when(blogService.convertToSecurePreviewResponse(other, 8L, false)).thenReturn(responseOf(other));

        BlogRecommendationResult result = service.getRecommendations(1L, 8L, 3);

        assertTrue(result.getItems().stream().noneMatch(item -> item.getId().equals(1L)));
        assertIterableEquals(List.of(4L), result.getItems().stream().map(BlogResponse::getId).toList());
    }

    private Blog buildBlog(Long id, Long authorId, String title, String summary, Map<String, String> tags) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setStatus("published");
        blog.setTitle(title);
        blog.setSummary(summary);
        blog.setTags(tags);
        blog.setCreatedAt(Instant.parse("2026-04-18T00:00:00Z"));
        blog.setPublishTime(Instant.parse("2026-04-19T00:00:00Z"));
        blog.setViewCount(10);
        blog.setLikeCount(2);
        blog.setCollectCount(1);
        blog.setDownloadCount(0);
        UserInfo author = new UserInfo();
        author.setId(authorId);
        author.setUsername("author-" + authorId);
        blog.setAuthor(author);
        return blog;
    }

    private BlogResponse responseOf(Blog blog) {
        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        return response;
    }

    private Map<String, String> tags(String... entries) {
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            tags.put(entries[index], entries[index + 1]);
        }
        return tags;
    }
}
