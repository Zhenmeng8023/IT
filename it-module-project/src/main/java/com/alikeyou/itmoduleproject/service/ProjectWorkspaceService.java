package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectWorkspaceService {
    ProjectWorkspaceVO getCurrentWorkspace(Long projectId, Long branchId, Long currentUserId);
    ProjectWorkspaceItemVO stageFile(Long projectId, Long branchId, Long currentUserId, String canonicalPath, MultipartFile file);
    ProjectWorkspaceItemVO stageDelete(Long projectId, Long branchId, Long currentUserId, String canonicalPath);
    List<ProjectWorkspaceItemVO> listItems(Long projectId, Long branchId, Long currentUserId);
    ProjectCommitVO commit(Long projectId, Long branchId, Long currentUserId, String message);
}
