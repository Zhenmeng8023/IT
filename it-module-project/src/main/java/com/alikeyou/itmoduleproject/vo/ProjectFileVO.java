package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectFileVO {

    private Long id;

    private Long projectId;

    private String fileName;

    private String filePath;

    private String relativePath;

    private Long fileSizeBytes;

    private String fileType;

    private LocalDateTime uploadTime;

    private Boolean isMain;

    private String version;

    private Boolean isLatest;

    private Long viewBranchId;

    private Boolean defaultBranchView;

    private List<ProjectFileVersionVO> versions;
}
