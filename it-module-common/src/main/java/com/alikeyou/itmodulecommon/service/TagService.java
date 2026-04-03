package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.dto.TagCategoryResponse;
import com.alikeyou.itmodulecommon.dto.TagRequest;
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
    List<TagCategoryResponse> getTagCategories();
    Page<Tag> getTagsPage(Pageable pageable);
    Tag createTag(TagRequest request);
    Tag updateTag(Long id, TagRequest request);
    void deleteTag(Long id);
}
