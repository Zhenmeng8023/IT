package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface TagService {
    List<Tag> getAllTags();
    Optional<Tag> getTagById(Long id);
    List<Tag> getTagsByIds(List<Long> ids);
    List<Tag> getHotTags(int limit);
    Page<Tag> getTagsPage(Pageable pageable);
    Tag createTag(Tag tag);
    Tag updateTag(Long id, Tag tag);
    void deleteTag(Long id);
}