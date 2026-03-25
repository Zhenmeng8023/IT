package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.creatorId = :userId AND c.aiType IS NOT NULL ORDER BY c.updatedAt DESC")
    List<Conversation> findUserAiConversations(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.creatorId = :userId AND c.aiType = :aiType ORDER BY c.updatedAt DESC")
    List<Conversation> findUserConversationsByAiType(@Param("userId") Long userId, @Param("aiType") String aiType);
}