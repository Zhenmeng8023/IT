package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectTemplateSourceVO {
    private Long projectId;
    private Integer docCount;
    private Integer taskCount;
    private Integer fileCount;
    private Integer activityCount;
    private List<String> fileSuffixOptions;
    private List<String> activityActionOptions;
    private List<ProjectTemplateSelectableItemVO> docs;
    private List<ProjectTemplateSelectableItemVO> tasks;
    private List<ProjectTemplateSelectableItemVO> files;
    private List<ProjectTemplateSelectableItemVO> activities;
}
