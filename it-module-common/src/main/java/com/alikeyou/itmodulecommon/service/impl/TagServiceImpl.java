package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmodulecommon.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagRepository tagRepository;

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    public Optional<Tag> getTagById(Long id) {
        return tagRepository.findById(id);
    }

    @Override
    public List<Tag> getTagsByIds(List<Long> ids) {
        return tagRepository.findAllById(ids);
    }

    @Override
    public List<Tag> getHotTags(int limit) {
        // 这里简单实现，后续可以根据实际使用频率进行排序
        return tagRepository.findAll().stream().limit(limit).toList();
    }

    @Override
    public Page<Tag> getTagsPage(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Override
    public Tag createTag(Tag tag) {
        return tagRepository.save(tag);
    }

    @Override
    public Tag updateTag(Long id, Tag tag) {
        Optional<Tag> existingTag = tagRepository.findById(id);
        if (existingTag.isPresent()) {
            Tag updatedTag = existingTag.get();
            updatedTag.setName(tag.getName());
            updatedTag.setParent(tag.getParent());
            updatedTag.setCategory(tag.getCategory());
            updatedTag.setDescription(tag.getDescription());
            return tagRepository.save(updatedTag);
        }
        return null;
    }

    @Override
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }
}