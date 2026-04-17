package com.alikeyou.itmoduleproject.dto;

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
public class ContentConflictResolveRequest {
    private String conflictId;
    private String resolvedContent;
    private List<BlockChoice> blockChoices;
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockChoice {
        private String blockId;
        private String choice;
        private List<String> resolvedLines;
        private String resolvedContent;
        private Map<String, Object> metadata;
    }
}
