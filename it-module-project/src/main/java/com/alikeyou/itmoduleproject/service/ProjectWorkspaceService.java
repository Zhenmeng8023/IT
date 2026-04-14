package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.support.diff.ChangeEntry;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectWorkspaceService {
    ProjectWorkspaceVO getCurrentWorkspace(Long projectId, Long branchId, Long currentUserId);
    ProjectWorkspaceItemVO stageFile(Long projectId, Long branchId, Long currentUserId, String canonicalPath, MultipartFile file);
    List<ProjectWorkspaceItemVO> stageFiles(Long projectId, Long branchId, Long currentUserId, String targetDir, List<MultipartFile> files, List<String> relativePaths);
    List<ProjectWorkspaceItemVO> stageZip(Long projectId, Long branchId, Long currentUserId, MultipartFile file);
    ProjectWorkspaceItemVO stageDelete(Long projectId, Long branchId, Long currentUserId, String canonicalPath);
    List<ChangeEntry> listItems(Long projectId, Long branchId, Long currentUserId);
    ProjectCommitVO commit(Long projectId, Long branchId, Long currentUserId, String message);
}
