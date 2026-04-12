package com.alikeyou.itmoduleinteractive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CountResponseDTO {
    private Long userId;
    private String metric;
    private long count;
}
