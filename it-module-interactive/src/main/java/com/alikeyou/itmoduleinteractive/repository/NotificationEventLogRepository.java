package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.NotificationEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationEventLogRepository extends JpaRepository<NotificationEventLog, Long> {

    Optional<NotificationEventLog> findByEventKey(String eventKey);
}
