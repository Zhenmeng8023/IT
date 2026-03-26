package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiFeedbackLogRepository extends JpaRepository<AiFeedbackLog, Long> {

    List<AiFeedbackLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AiFeedbackLog> findByMessage_IdOrderByCreatedAtDesc(Long messageId);
}
