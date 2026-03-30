package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class BlogUpdateRequest {
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private List<Long> tagIds;
    private String status;
    private Boolean isMarked;
    private Instant publishTime;
    
    /**
     * 博客价格：0 为免费，-1 为 VIP 专属，其他值为付费价格
     */
    private Integer price;
}