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

    /**
     * 使用@EntityGraph解决N+1查询问题，同时加载作者和项目信息
     */
    @EntityGraph(attributePaths = {"author", "project"})
    Optional<Blog> findWithAssociationsById(Long id);

    /**
     * 获取所有博客并加载关联信息
     */
    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"author", "project"})
    List<Blog> findAll();

    /**
     * 根据作者 ID 查询草稿博客列表
     * @param authorId 作者 ID
     * @return 该作者的草稿博客列表，按更新时间降序排列
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'draft' AND b.author.id = :authorId ORDER BY b.updatedAt DESC")
    List<Blog> findDraftBlogsByAuthorId(@Param("authorId") Long authorId);


    /**
     * 根据作者ID查询博客
     */
    @EntityGraph(attributePaths = {"author", "project"})
    List<Blog> findByAuthorId(Long authorId);

    /**
     * 原子操作：增加浏览次数
     */
    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 原子操作：增加点赞次数
     */
    @Modifying
    @Query("UPDATE Blog b SET b.likeCount = b.likeCount + 1 WHERE b.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /**
     * 原子操作：增加收藏次数
     */
    @Modifying
    @Query("UPDATE Blog b SET b.collectCount = b.collectCount + 1 WHERE b.id = :id")
    void incrementCollectCount(@Param("id") Long id);

    /**
     * 原子操作：增加下载次数
     */
    @Modifying
    @Query("UPDATE Blog b SET b.downloadCount = b.downloadCount + 1 WHERE b.id = :id")
    void incrementDownloadCount(@Param("id") Long id);

    /**
     * 搜索博客（标题和内容）- 返回所有匹配的结果
     */

    @Query("SELECT b FROM Blog b WHERE b.status = 'published' AND " +
            "(b.title LIKE %:keyword% OR b.content LIKE %:keyword%)")
    List<Blog> searchBlogs(@Param("keyword") String keyword);

    /**
     * 搜索博客（标签）- tags 是 Map 结构，需要搜索所有的 values
     */
    @Query(value = "SELECT * FROM blog b WHERE b.status = 'published' AND " +
            "b.tags LIKE CONCAT('%', :keyword, '%')", nativeQuery = true)
    List<Blog> searchBlogsByTag(@Param("keyword") String keyword);

    /**
     * 搜索博客（作者用户名）- 返回所有匹配的作者的博客
     */
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' AND " +
            "b.author.username LIKE CONCAT('%', :keyword, '%')")
    List<Blog> searchBlogsByAuthor(@Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY b.publishTime DESC")
    List<Blog> findPublishedBlogs();

    /**
     * 获取所有草稿博客
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'draft' ORDER BY b.updatedAt DESC")
    List<Blog> findDraftBlogs();

    /**
     * 按热度排序获取博客列表
     * 热度计算公式：viewCount * 1 + likeCount * 5 + collectCount * 10 + downloadCount * 8
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' " +
            "ORDER BY (b.viewCount * 1 + b.likeCount * 5 + b.collectCount * 10 + b.downloadCount * 8) DESC, b.publishTime DESC")
    List<Blog> findByHotness();

    /**
     * 按发布时间排序获取博客列表（最新在前）
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY b.publishTime DESC")
    List<Blog> findByTimeDesc();

    /**
     * 按发布时间排序获取博客列表（最旧在前）
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'published' ORDER BY b.publishTime ASC")
    List<Blog> findByTimeAsc();

    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'rejected' ORDER BY b.updatedAt DESC")
    List<Blog> findRejectedBlogs();

    /**
     * 获取待审核博客列表
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'pending' ORDER BY b.updatedAt DESC")
    List<Blog> findPendingBlogs();

    /**
     * 分页获取待审核博客列表
     */
    @EntityGraph(attributePaths = {"author", "project"})
    @Query("SELECT b FROM Blog b WHERE b.status = 'pending' ORDER BY b.updatedAt DESC")
    org.springframework.data.domain.Page<Blog> findPendingBlogs(org.springframework.data.domain.Pageable pageable);

    /**
     * 原子操作：减少收藏次数
     */
    @Modifying
    @Query("UPDATE Blog b SET b.collectCount = b.collectCount - 1 WHERE b.id = :id")
    void decrementCollectCount(@Param("id") Long id);

}