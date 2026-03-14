package com.alikeyou.itmodulecircle.repository;

import com.alikeyou.itmodulecircle.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByType(String type);

    List<Conversation> findByCreatorId(Long creatorId);

    Optional<Conversation> findByNameAndType(String name, String type);

    boolean existsByNameAndType(String name, String type);
}
