package com.alikeyou.itmodulecommon.dto;

public class TagCategoryResponse {

    private String id;

    private String name;

    private long tagCount;

    public TagCategoryResponse() {
    }

    public TagCategoryResponse(String id, String name, long tagCount) {
        this.id = id;
        this.name = name;
        this.tagCount = tagCount;
    }

    public static TagCategoryResponse of(String name, long tagCount) {
        return new TagCategoryResponse(name, name, tagCount);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTagCount() {
        return tagCount;
    }

    public void setTagCount(long tagCount) {
        this.tagCount = tagCount;
    }
}
