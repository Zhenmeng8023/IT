package com.alikeyou.itmodulecommon.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminBatchUserStatusDTO {
    private List<Long> userIds;
    private String status;
}
