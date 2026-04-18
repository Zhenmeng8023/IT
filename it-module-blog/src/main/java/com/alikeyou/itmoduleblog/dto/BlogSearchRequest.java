package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

@Data
public class BlogSearchRequest {
    private String keyword;
    private String scope;
    private String sort;
    private String status;
}
