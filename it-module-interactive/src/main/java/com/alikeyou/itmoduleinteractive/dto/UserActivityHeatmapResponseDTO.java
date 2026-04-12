package com.alikeyou.itmoduleinteractive.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UserActivityHeatmapResponseDTO {
    private Long userId;
    private int days;
    private String startDate;
    private String endDate;
    private int totalCount;
    private String busiestDate;
    private int busiestCount;
    private Map<String, Integer> summary = new LinkedHashMap<>();
    private List<UserActivityHeatmapDayDTO> activities = new ArrayList<>();
}
