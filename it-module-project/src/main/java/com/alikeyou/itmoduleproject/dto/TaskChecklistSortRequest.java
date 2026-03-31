package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskChecklistSortRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Integer sortOrder;
    }
}
