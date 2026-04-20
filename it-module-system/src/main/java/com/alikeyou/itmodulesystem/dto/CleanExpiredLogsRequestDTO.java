package com.alikeyou.itmodulesystem.dto;

import lombok.Data;

@Data
public class CleanExpiredLogsRequestDTO {
    private Integer retainDays;
}
