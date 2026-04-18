package com.alikeyou.itmodulecircle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircleManagePageData<T> {

    private List<T> list;

    private Long total;

    private Integer currentPage;

    private Integer pageSize;
}

