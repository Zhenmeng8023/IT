package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectDocCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectDocUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectDocListItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionVO;

import java.util.List;

public interface ProjectDocService {

    List<ProjectDocListItemVO> listDocs(Long projectId, String type, String keyword, String status, String visibility, Long currentUserId);

    ProjectDocVO getPrimaryReadmeDoc(Long projectId, Long currentUserId);

    ProjectDocVO createDoc(Long projectId, ProjectDocCreateRequest request, Long currentUserId);

    ProjectDocVO getDoc(Long docId, Long currentUserId);

    ProjectDocVO updateDoc(Long docId, ProjectDocUpdateRequest request, Long currentUserId);

    void deleteDoc(Long docId, Long currentUserId);

    List<ProjectDocVersionVO> listVersions(Long docId, Long currentUserId);

    ProjectDocVersionVO getVersion(Long docId, Integer versionNo, Long currentUserId);

    ProjectDocVO rollback(Long docId, Integer versionNo, Long currentUserId);
}