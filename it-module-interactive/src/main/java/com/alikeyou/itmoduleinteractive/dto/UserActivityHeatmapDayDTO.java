package com.alikeyou.itmoduleinteractive.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class UserActivityHeatmapDayDTO {
    private String date;
    private int count;
    private Map<String, Integer> breakdown = new LinkedHashMap<>();
}
