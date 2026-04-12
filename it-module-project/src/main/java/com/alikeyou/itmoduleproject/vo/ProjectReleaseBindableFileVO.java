package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectReleaseBindableFileVO {
    private Long projectFileId;
    private Long projectFileVersionId;
    private String canonicalPath;
    private String fileName;
    private String version;
    private String fileType;
    private Long fileSizeBytes;
    private String commitMessage;
}
