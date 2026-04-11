package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt ASC")
    List<Message> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt ASC")
    List<Message> findByConversationIdOrderBySentAtAsc(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt DESC")
    List<Message> findByConversationIdOrderBySentAtDesc(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.isRead = false")
    List<Message> findByConversationIdAndIsReadFalse(@Param("conversationId") Long conversationId);

    Message findFirstByConversationIdOrderBySentAtDescIdDesc(Long conversationId);

    @Query("""
            SELECT COUNT(m)
            FROM Message m
            WHERE m.conversationId = :conversationId
              AND m.senderId <> :userId
              AND (:lastReadAt IS NULL OR m.sentAt > :lastReadAt)
            """)
    long countUnreadForUser(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId,
                            @Param("lastReadAt") Instant lastReadAt);

    long countByConversationId(Long conversationId);

    long countByConversationIdAndIsReadFalse(@Param("conversationId") Long conversationId);
}
