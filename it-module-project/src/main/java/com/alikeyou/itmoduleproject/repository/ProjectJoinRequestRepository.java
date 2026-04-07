
package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectJoinRequestRepository extends JpaRepository<ProjectJoinRequest, Long> {

    List<ProjectJoinRequest> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<ProjectJoinRequest> findFirstByProjectIdAndApplicantIdOrderByCreatedAtDesc(Long projectId, Long applicantId);

    boolean existsByProjectIdAndApplicantIdAndStatus(Long projectId, Long applicantId, String status);
}
