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
public class ConflictDetail {
    private ConflictType conflictType;
    private ChangeType sourceChangeType;
    private ChangeType targetChangeType;
    private String oldPath;
    private String newPath;
    private String fileName;
    private String basePath;
    private String sourcePath;
    private String targetPath;
    private Long baseCommitId;
    private Long sourceCommitId;
    private Long targetCommitId;
    private String contentHashBefore;
    private String contentHashAfter;
    private String baseContentHash;
    private String sourceContentHash;
    private String targetContentHash;
    private String summary;
    private String suggestedAction;
    private String severity;
    private List<ChangeEntry> relatedChanges;
    private Map<String, Object> metadata;
}
