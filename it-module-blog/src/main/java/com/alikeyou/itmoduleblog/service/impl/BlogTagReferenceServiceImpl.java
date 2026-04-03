package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.service.TagReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BlogTagReferenceServiceImpl implements TagReferenceService {

    @Autowired
    private BlogRepository blogRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countPublishedTagUsage() {
        return countTagUsage(blogRepository.findPublishedBlogs());
    }

    @Override
    @Transactional
    public void renameTagReferences(Long tagId, String newTagName) {
        if (tagId == null || newTagName == null || newTagName.trim().isEmpty()) {
            return;
        }

        String tagKey = String.valueOf(tagId);
        List<Blog> changedBlogs = new ArrayList<>();
        for (Blog blog : blogRepository.findAll()) {
            Map<String, String> tags = blog.getTags();
            if (tags == null || !tags.containsKey(tagKey)) {
                continue;
            }

            if (Objects.equals(tags.get(tagKey), newTagName)) {
                continue;
            }

            Map<String, String> updatedTags = new LinkedHashMap<>(tags);
            updatedTags.put(tagKey, newTagName);
            blog.setTags(updatedTags);
            blog.setUpdatedAt(Instant.now());
            changedBlogs.add(blog);
        }

        if (!changedBlogs.isEmpty()) {
            blogRepository.saveAll(changedBlogs);
        }
    }

    @Override
    @Transactional
    public void removeTagReferences(Long tagId) {
        if (tagId == null) {
            return;
        }

        String tagKey = String.valueOf(tagId);
        List<Blog> changedBlogs = new ArrayList<>();
        for (Blog blog : blogRepository.findAll()) {
            Map<String, String> tags = blog.getTags();
            if (tags == null || !tags.containsKey(tagKey)) {
                continue;
            }

            Map<String, String> updatedTags = new LinkedHashMap<>(tags);
            updatedTags.remove(tagKey);
            blog.setTags(updatedTags.isEmpty() ? null : updatedTags);
            blog.setUpdatedAt(Instant.now());
            changedBlogs.add(blog);
        }

        if (!changedBlogs.isEmpty()) {
            blogRepository.saveAll(changedBlogs);
        }
    }

    private Map<Long, Long> countTagUsage(List<Blog> blogs) {
        Map<Long, Long> usageCountMap = new HashMap<>();
        for (Blog blog : blogs) {
            if (blog.getTags() == null || blog.getTags().isEmpty()) {
                continue;
            }

            for (String tagId : blog.getTags().keySet()) {
                Long parsedTagId = parseTagId(tagId);
                if (parsedTagId == null) {
                    continue;
                }
                usageCountMap.merge(parsedTagId, 1L, Long::sum);
            }
        }
        return usageCountMap;
    }

    private Long parseTagId(String rawTagId) {
        if (rawTagId == null || rawTagId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawTagId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
