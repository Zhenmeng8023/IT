package com.alikeyou.itmoduleblog.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class BlogUpdateRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private Map<String, Object> tags;
    private Long projectId;
    private String status;
    private Boolean isMarked;
    private Instant publishTime;
}