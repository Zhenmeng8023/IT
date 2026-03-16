package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.util.List;

@Data
public class BlogCreateRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private List<Long> tagIds;
    private Long projectId;
    private String status;
}
