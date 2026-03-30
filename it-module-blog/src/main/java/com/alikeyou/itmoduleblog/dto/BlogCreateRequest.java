package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.util.List;

@Data
public class BlogCreateRequest {
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private List<Long> tagIds;
    private String status;
    
    /**
     * 博客价格：0 为免费，-1 为 VIP 专属，其他值为付费价格
     */
    private Integer price;
}