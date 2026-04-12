package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectFileService {

    ProjectWorkspaceItemVO uploadFile(Long projectId, Long branchId, String canonicalPath, MultipartFile file, Long currentUserId);

    List<ProjectWorkspaceItemVO> uploadZip(Long projectId, Long branchId, MultipartFile file, Long currentUserId);

    List<ProjectWorkspaceItemVO> uploadFiles(Long projectId, Long branchId, String targetDir, List<MultipartFile> files, Long currentUserId);

    ProjectWorkspaceItemVO uploadNewVersion(Long fileId, Long branchId, MultipartFile file, Long currentUserId);

    List<ProjectFileVO> listFiles(Long projectId, Long currentUserId);

    List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId);

    Resource previewFile(Long fileId, Long currentUserId);

    Resource downloadFile(Long fileId, Long currentUserId);

    Resource downloadFiles(Long projectId, List<Long> fileIds, Long currentUserId);

    ProjectFileVO setMainFile(Long fileId, Long currentUserId);

    ProjectWorkspaceItemVO deleteFile(Long fileId, Long branchId, Long currentUserId);
}
