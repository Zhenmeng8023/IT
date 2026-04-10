package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectStatDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjectStatDailyRepository extends JpaRepository<ProjectStatDaily, Long> {

    Optional<ProjectStatDaily> findByProjectIdAndStatDate(Long projectId, LocalDate statDate);

    List<ProjectStatDaily> findByProjectIdAndStatDateBetweenOrderByStatDateAsc(Long projectId, LocalDate startDate, LocalDate endDate);

    Page<ProjectStatDaily> findByProjectIdOrderByStatDateDesc(Long projectId, Pageable pageable);
}
