package com.alikeyou.itmoduleinteractive.repository;

import com.alikeyou.itmoduleinteractive.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndType(Long id, String type);

    List<Conversation> findByCreatorId(Long creatorId);

    List<Conversation> findByType(String type);

    List<Conversation> findByTypeAndCreatorId(String type, Long creatorId);
}
