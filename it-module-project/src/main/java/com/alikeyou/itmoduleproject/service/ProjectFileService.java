package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectFileService {

    ProjectFileVO uploadFile(Long projectId, MultipartFile file, Boolean isMain, String version, String commitMessage, Long currentUserId);

    List<ProjectFileVO> uploadZip(Long projectId, MultipartFile file, String version, String commitMessage, Long currentUserId);

    List<ProjectFileVO> uploadFiles(Long projectId, List<MultipartFile> files, Integer mainFileIndex, String version, String commitMessage, Long currentUserId);

    ProjectFileVO uploadNewVersion(Long fileId, MultipartFile file, String version, String commitMessage, Long currentUserId);

    List<ProjectFileVO> listFiles(Long projectId, Long currentUserId);

    List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId);

    Resource previewFile(Long fileId, Long currentUserId);

    Resource downloadFile(Long fileId, Long currentUserId);

    Resource downloadFiles(Long projectId, List<Long> fileIds, Long currentUserId);

    ProjectFileVO setMainFile(Long fileId, Long currentUserId);

    void deleteFile(Long fileId, Long currentUserId);
}
