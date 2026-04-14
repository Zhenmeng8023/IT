package com.alikeyou.itmoduleproject.support.diff;

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
public class ContentConflictDetail {
    private Long mergeRequestId;
    private String conflictId;
    private ConflictType conflictType;
    private String path;
    private String fileName;
    private Long baseCommitId;
    private Long sourceCommitId;
    private Long targetCommitId;
    private String baseContent;
    private String sourceContent;
    private String targetContent;
    private Integer baseLineCount;
    private Integer sourceLineCount;
    private Integer targetLineCount;
    private Boolean binaryFile;
    private String summary;
    private String suggestedAction;
    private List<ContentConflictBlock> blocks;
    private Map<String, Object> metadata;
}
