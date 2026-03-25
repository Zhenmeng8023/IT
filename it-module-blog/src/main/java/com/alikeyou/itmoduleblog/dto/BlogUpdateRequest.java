package com.alikeyou.itmoduleblog.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class BlogUpdateRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private List<Long> tagIds;
    private String status;
    private Boolean isMarked;
    private Instant publishTime;
}