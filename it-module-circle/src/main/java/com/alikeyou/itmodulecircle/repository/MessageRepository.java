package com.alikeyou.itmodulecircle.repository;

import com.alikeyou.itmodulecircle.entity.Conversation;
import com.alikeyou.itmodulecircle.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    List<Message> findByConversationIdAndSentAtAfterOrderBySentAtAsc(Long conversationId, java.time.Instant sentAt);

    List<Message> findBySenderId(Long senderId);

    long countByConversationIdAndIsReadFalse(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
