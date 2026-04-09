package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    List<ProjectInvitation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectInvitation> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);

    List<ProjectInvitation> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, String status);

    Optional<ProjectInvitation> findByInviteCode(String inviteCode);

    Optional<ProjectInvitation> findByIdAndInviteeId(Long id, Long inviteeId);

    Optional<ProjectInvitation> findByIdAndProjectId(Long id, Long projectId);

    Optional<ProjectInvitation> findFirstByProjectIdAndInviteeIdAndStatusOrderByCreatedAtDesc(Long projectId, Long inviteeId, String status);

    boolean existsByProjectIdAndInviteeIdAndStatus(Long projectId, Long inviteeId, String status);
}
