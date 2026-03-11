package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Tag;
import java.util.List;
import java.util.Optional;

public interface TagService {
    List<Tag> getAllTags();
    Optional<Tag> getTagById(Long id);
}