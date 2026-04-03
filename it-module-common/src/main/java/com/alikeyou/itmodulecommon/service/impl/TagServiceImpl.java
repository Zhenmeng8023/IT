package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.dto.TagCategoryResponse;
import com.alikeyou.itmodulecommon.dto.TagRequest;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmodulecommon.service.TagReferenceService;
import com.alikeyou.itmodulecommon.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired(required = false)
    private TagReferenceService tagReferenceService;

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> getTagById(Long id) {
        return tagRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTagsByIds(List<Long> ids) {
        return tagRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getHotTags(int limit) {
        Map<Long, Long> usageCountMap = getTagUsageCountMap();
        List<Tag> tags = new ArrayList<>(tagRepository.findAllByOrderByNameAsc());
        tags.sort(
                Comparator.comparingLong((Tag tag) -> usageCountMap.getOrDefault(tag.getId(), 0L)).reversed()
                        .thenComparing(tag -> normalizeText(tag.getName()))
        );

        if (limit <= 0 || tags.size() <= limit) {
            return tags;
        }
        return tags.subList(0, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagCategoryResponse> getTagCategories() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(Tag::getCategory)
                .map(this::normalizeOptionalText)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(category -> category, TreeMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> TagCategoryResponse.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tag> getTagsPage(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Tag createTag(TagRequest request) {
        String tagName = validateName(request);
        ensureUniqueName(tagName, null);

        Tag tag = new Tag();
        tag.setName(tagName);
        tag.setParent(resolveParent(request.getParentId(), null));
        tag.setCategory(normalizeOptionalText(request.getCategory()));
        tag.setDescription(normalizeOptionalText(request.getDescription()));
        tag.setCreatedAt(Instant.now());
        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag updateTag(Long id, TagRequest request) {
        Tag existingTag = tagRepository.findById(id)
                .orElseThrow(() -> notFound("标签不存在，ID: " + id));

        String oldName = existingTag.getName();
        String tagName = validateName(request);
        ensureUniqueName(tagName, id);

        existingTag.setName(tagName);
        existingTag.setParent(resolveParent(request.getParentId(), id));
        existingTag.setCategory(normalizeOptionalText(request.getCategory()));
        existingTag.setDescription(normalizeOptionalText(request.getDescription()));

        Tag savedTag = tagRepository.save(existingTag);
        if (tagReferenceService != null && !Objects.equals(oldName, savedTag.getName())) {
            tagReferenceService.renameTagReferences(savedTag.getId(), savedTag.getName());
        }
        return savedTag;
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag existingTag = tagRepository.findById(id)
                .orElseThrow(() -> notFound("标签不存在，ID: " + id));

        detachChildTags(id);
        if (tagReferenceService != null) {
            tagReferenceService.removeTagReferences(id);
        }
        tagRepository.delete(existingTag);
    }

    private void detachChildTags(Long parentId) {
        List<Tag> childTags = tagRepository.findAll().stream()
                .filter(tag -> tag.getParent() != null && Objects.equals(tag.getParent().getId(), parentId))
                .toList();
        if (childTags.isEmpty()) {
            return;
        }

        childTags.forEach(tag -> tag.setParent(null));
        tagRepository.saveAll(childTags);
    }

    private Tag resolveParent(Long parentId, Long currentTagId) {
        if (parentId == null) {
            return null;
        }
        if (Objects.equals(parentId, currentTagId)) {
            throw badRequest("标签不能将自己设为父标签");
        }
        return tagRepository.findById(parentId)
                .orElseThrow(() -> notFound("父标签不存在，ID: " + parentId));
    }

    private void ensureUniqueName(String tagName, Long currentTagId) {
        tagRepository.findByNameIgnoreCase(tagName).ifPresent(existing -> {
            if (currentTagId == null || !Objects.equals(existing.getId(), currentTagId)) {
                throw badRequest("标签名称已存在：" + tagName);
            }
        });
    }

    private String validateName(TagRequest request) {
        if (request == null) {
            throw badRequest("标签参数不能为空");
        }

        String normalizedName = normalizeText(request.getName());
        if (normalizedName.isEmpty()) {
            throw badRequest("标签名称不能为空");
        }
        if (normalizedName.length() > 50) {
            throw badRequest("标签名称长度不能超过 50 个字符");
        }
        return normalizedName;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<Long, Long> getTagUsageCountMap() {
        if (tagReferenceService == null) {
            return Collections.emptyMap();
        }
        return tagReferenceService.countPublishedTagUsage();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
