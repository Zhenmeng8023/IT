package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class BlogSearchResult {
    private String keyword;
    private String scope;
    private String sort;
    private String status;
    private Integer total;
    private List<BlogResponse> items = Collections.emptyList();
}
