package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRetrievalLogRepository extends JpaRepository<AiRetrievalLog, Long> {

    @Query("SELECT arl FROM AiRetrievalLog arl WHERE arl.callLog.id = :callLogId ORDER BY arl.rankNo ASC")
    List<AiRetrievalLog> findByCallLogId(@Param("callLogId") Long callLogId);

    @Query("SELECT arl FROM AiRetrievalLog arl WHERE arl.callLog.id = :callLogId AND arl.knowledgeBase.id = :kbId")
    List<AiRetrievalLog> findByCallLogIdAndKnowledgeBaseId(@Param("callLogId") Long callLogId, @Param("kbId") Long kbId);
}