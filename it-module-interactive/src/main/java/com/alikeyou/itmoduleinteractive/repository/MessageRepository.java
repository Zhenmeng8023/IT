package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    List<Message> findByConversationIdOrderBySentAtDesc(Long conversationId);

    List<Message> findBySenderId(Long senderId);

    List<Message> findByConversationIdAndIsReadFalse(Long conversationId);

    long countByConversationIdAndIsReadFalse(Long conversationId);
}
