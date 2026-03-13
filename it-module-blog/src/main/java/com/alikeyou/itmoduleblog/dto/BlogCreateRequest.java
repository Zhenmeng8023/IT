package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BlogCreateRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private Map<String, Object> tags;
    private Long projectId;
}