package com.alikeyou.itmodulesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleanExpiredLogsResultDTO {
    private long deletedCount;
    private int retainDays;
}
