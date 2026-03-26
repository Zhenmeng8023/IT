package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRetrievalLogRepository extends JpaRepository<AiRetrievalLog, Long> {

    List<AiRetrievalLog> findByCallLog_IdOrderByRankNoAsc(Long callLogId);
}
