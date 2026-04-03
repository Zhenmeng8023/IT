package com.alikeyou.itmodulecommon.dto;

import com.alikeyou.itmodulecommon.entity.Tag;

import java.time.Instant;

public class TagResponse {

    private Long id;

    private String name;

    private Long parentId;

    private String parentName;

    private String category;

    private String description;

    private Instant createdAt;

    private long useCount;

    public static TagResponse from(Tag tag, long useCount) {
        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setCategory(tag.getCategory());
        response.setDescription(tag.getDescription());
        response.setCreatedAt(tag.getCreatedAt());
        response.setUseCount(useCount);

        if (tag.getParent() != null) {
            response.setParentId(tag.getParent().getId());
            response.setParentName(tag.getParent().getName());
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getUseCount() {
        return useCount;
    }

    public void setUseCount(long useCount) {
        this.useCount = useCount;
    }
}
