package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {
}
