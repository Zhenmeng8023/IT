package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BlogDashboardRankVO {
    private List<BlogResponse> hotBlogs = new ArrayList<>();
    private List<RankItem> hotAuthors = new ArrayList<>();
    private List<RankItem> hotTags = new ArrayList<>();

    @Data
    public static class RankItem {
        private Long id;
        private String name;
        private Long count;
        private Long heat;
        private String extra;
    }
}
