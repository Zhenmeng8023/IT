
package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    List<ProjectInvitation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<ProjectInvitation> findByInviteCode(String inviteCode);

    Optional<ProjectInvitation> findFirstByProjectIdAndInviteeIdAndStatusOrderByCreatedAtDesc(Long projectId, Long inviteeId, String status);

    boolean existsByProjectIdAndInviteeIdAndStatus(Long projectId, Long inviteeId, String status);
}
