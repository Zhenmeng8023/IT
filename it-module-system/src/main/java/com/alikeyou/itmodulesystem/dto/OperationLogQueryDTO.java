package com.alikeyou.itmodulesystem.dto;

import lombok.Data;

@Data
public class OperationLogQueryDTO {
    private Integer page = 1;
    private Integer size = 20;
    private String type;
    private String operator;
    private String module;
    private String startTime;
    private String endTime;
}
