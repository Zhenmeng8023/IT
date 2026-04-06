package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

@Data
public class BlogInteractionSummaryVO {
    private Long blogId;
    private Integer likeCount;
    private Integer collectCount;
    private Integer reportCount;
    private Boolean liked;
    private Boolean collected;
}
