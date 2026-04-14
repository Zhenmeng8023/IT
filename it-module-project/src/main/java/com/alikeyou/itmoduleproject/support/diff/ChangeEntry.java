package com.alikeyou.itmoduleproject.support.diff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEntry {
    private ChangeType changeType;
    private String oldPath;
    private String newPath;
    private String fileName;
    private String contentHashBefore;
    private String contentHashAfter;
    private Long oldBlobId;
    private Long newBlobId;
    private Long baseCommitId;
    private Long sourceCommitId;
    private Long targetCommitId;
    private String summary;
    private String suggestedAction;
    private String severity;
    private Map<String, Object> metadata;
}
