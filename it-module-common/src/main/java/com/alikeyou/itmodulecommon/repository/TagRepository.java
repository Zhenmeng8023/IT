package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByOrderByNameAsc();

    Optional<Tag> findByNameIgnoreCase(String name);
}
