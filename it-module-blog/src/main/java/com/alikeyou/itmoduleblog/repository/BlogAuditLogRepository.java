
package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.BlogAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogAuditLogRepository extends JpaRepository<BlogAuditLog, Long> {

    BlogAuditLog findTopByBlogIdOrderByIdDesc(Long blogId);

    BlogAuditLog findTopByBlogIdAndAuditStatusOrderByIdDesc(Long blogId, String auditStatus);

    List<BlogAuditLog> findByBlogIdOrderByIdDesc(Long blogId);
}
