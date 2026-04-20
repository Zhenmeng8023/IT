package com.alikeyou.itmodulesystem.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class OperationLogItemDTO {
    private Long id;
    private Instant createTime;
    private String operator;
    private String type;
    private String module;
    private String action;
    private String ip;
    private String result;
    private Object details;
}
