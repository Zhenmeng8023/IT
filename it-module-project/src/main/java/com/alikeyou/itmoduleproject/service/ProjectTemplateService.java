package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectTemplateApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateSaveRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateSourceVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateVO;

import java.util.List;

public interface ProjectTemplateService {

    List<ProjectTemplateVO> listTemplates(String keyword, Boolean mineOnly, Long currentUserId);

    ProjectTemplateDetailVO getTemplateDetail(Long templateId, Long currentUserId);

    ProjectTemplateSourceVO getTemplateSource(Long projectId, Long currentUserId);

    ProjectTemplateVO createTemplate(ProjectTemplateCreateRequest request, Long currentUserId);

    ProjectTemplateVO updateTemplate(Long templateId, ProjectTemplateUpdateRequest request, Long currentUserId);

    void deleteTemplate(Long templateId, Long currentUserId);

    ProjectDetailVO applyTemplate(Long templateId, ProjectTemplateApplyRequest request, Long currentUserId);

    ProjectTemplateVO saveProjectAsTemplate(Long projectId, ProjectTemplateSaveRequest request, Long currentUserId);
}
