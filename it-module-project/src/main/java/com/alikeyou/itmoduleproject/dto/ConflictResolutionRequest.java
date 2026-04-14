package com.alikeyou.itmoduleproject.dto;

import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResolutionRequest {
    private List<ConflictResolutionOption> options;
    private Boolean recheckAfterResolve;
    private Map<String, Object> metadata;
}
