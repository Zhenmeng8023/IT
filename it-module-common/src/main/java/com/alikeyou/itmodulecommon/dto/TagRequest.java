package com.alikeyou.itmodulecommon.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class TagRequest {

    private String name;

    @JsonAlias({"parent_id", "parentId"})
    private Long parentId;

    private String category;

    private String description;

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
}
