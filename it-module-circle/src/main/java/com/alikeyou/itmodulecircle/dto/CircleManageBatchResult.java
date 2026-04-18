package com.alikeyou.itmodulecircle.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class CircleManageBatchResult {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failedCount = 0;

    private List<Long> failedIds = new ArrayList<>();

    private Map<Long, String> failedReason = new LinkedHashMap<>();
}

