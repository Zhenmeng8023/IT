package com.alikeyou.itmoduleai.dto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConnectionTestResult {

    private boolean success;
    private String message;
    private String targetUrl;
    private Integer httpStatus;
    private Long durationMs;
    private String detail;
    private Instant testedAt;
}
