package com.alikeyou.itmoduleproject.support.diff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResolutionOption {
    private String conflictId;
    private String resolutionStrategy;
    private String targetPath;
    private String note;
    private Map<String, Object> metadata;

    public String normalizedStrategy() {
        if (resolutionStrategy == null || resolutionStrategy.isBlank()) {
            return null;
        }
        return resolutionStrategy.trim().toUpperCase(Locale.ROOT);
    }
}
