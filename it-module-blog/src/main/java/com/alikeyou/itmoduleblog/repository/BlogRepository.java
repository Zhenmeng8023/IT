
package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    @EntityGraph(attributePaths = {"author"})
    Optional<Blog> findWithAssociationsById(Long id);

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"author"})
    List<Blog> findAll();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status IN ('draft','rejected') AND b.author.id = :authorId ORDER BY b.updatedAt DESC")
    List<Blog> findDraftBlogsByAuthorId(@Param("authorId") Long authorId);

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByIdIn(List<Long> ids);

    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Blog b SET b.likeCount = b.likeCount + 1 WHERE b.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Blog b SET b.collectCount = b.collectCount + 1 WHERE b.id = :id")
    void incrementCollectCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Blog b SET b.downloadCount = b.downloadCount + 1 WHERE b.id = :id")
    void incrementDownloadCount(@Param("id") Long id);

    @Query("SELECT b FROM Blog b WHERE b.status = 'published' AND (b.title LIKE %:keyword% OR b.content LIKE %:keyword%)")
    List<Blog> searchBlogs(@Param("keyword") String keyword);

    @Query(value = "SELECT * FROM blog b WHERE b.status = 'published' AND b.tags LIKE CONCAT('%', :keyword, '%')", nativeQuery = true)
    List<Blog> searchBlogsByTag(@Param("keyword") String keyword);

    @Query("SELECT b FROM Blog b WHERE b.status = 'published' AND (b.author.nickname LIKE CONCAT('%', :keyword, '%') OR b.author.username LIKE CONCAT('%', :keyword, '%'))")
    List<Blog> searchBlogsByAuthor(@Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY b.publishTime DESC")
    List<Blog> findPublishedBlogs();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'draft' ORDER BY b.updatedAt DESC")
    List<Blog> findDraftBlogs();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY (b.viewCount * 1 + b.likeCount * 5 + b.collectCount * 10 + b.downloadCount * 8) DESC, COALESCE(b.publishTime, b.createdAt) DESC")
    List<Blog> findByHotness();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY COALESCE(b.publishTime, b.createdAt) DESC")
    List<Blog> findByTimeDesc();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY COALESCE(b.publishTime, b.createdAt) ASC")
    List<Blog> findByTimeAsc();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'rejected' ORDER BY b.updatedAt DESC")
    List<Blog> findRejectedBlogs();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'pending' ORDER BY b.updatedAt DESC")
    List<Blog> findPendingBlogs();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'pending' ORDER BY b.updatedAt DESC")
    org.springframework.data.domain.Page<Blog> findPendingBlogs(org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE Blog b SET b.collectCount = CASE WHEN b.collectCount > 0 THEN b.collectCount - 1 ELSE 0 END WHERE b.id = :id")
    void decrementCollectCount(@Param("id") Long id);
}
