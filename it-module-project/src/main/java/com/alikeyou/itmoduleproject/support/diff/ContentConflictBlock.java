package com.alikeyou.itmoduleproject.support.diff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentConflictBlock {
    private String blockId;
    private String blockType;
    private Integer baseStartLine;
    private Integer baseLineCount;
    private Integer sourceStartLine;
    private Integer sourceLineCount;
    private Integer targetStartLine;
    private Integer targetLineCount;
    private List<String> baseLines;
    private List<String> sourceLines;
    private List<String> targetLines;
    private String defaultChoice;
    private List<String> resolvedLines;
    private String resolvedContent;
}
