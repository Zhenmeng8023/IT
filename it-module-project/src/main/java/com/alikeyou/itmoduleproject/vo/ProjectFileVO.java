package com.alikeyou.itmoduleproject.vo;

import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
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
    private Long fileSizeBytes;
    private String fileType;
    private LocalDateTime uploadTime;
    private Boolean isMain;
    private String version;
    private Boolean isLatest;
    private List<ProjectFileVersionVO> versions;
}
