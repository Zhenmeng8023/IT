package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversationIdOrderByJoinedAtAsc(Long conversationId);

    List<ConversationParticipant> findByConversationIdAndUserIdNot(Long conversationId, Long userId);

    List<ConversationParticipant> findByUserIdOrderByJoinedAtDesc(Long userId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    void deleteByConversationId(Long conversationId);
}
