package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectDocCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectDocUpdateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectDocListItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionCompareVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionVO;

import java.util.List;

public interface ProjectDocService {

    List<ProjectDocListItemVO> listDocs(Long projectId, String type, String keyword, String status, String visibility, String isPrimary, Long currentUserId);

    List<ProjectDocListItemVO> listSidebarDocs(Long projectId, Long currentUserId);

    ProjectDocVO getPrimaryReadmeDoc(Long projectId, Long currentUserId);

    ProjectDocVO createDoc(Long projectId, ProjectDocCreateRequest request, Long currentUserId);

    ProjectDocVO getDoc(Long docId, Long currentUserId);

    ProjectDocVO updateDoc(Long docId, ProjectDocUpdateRequest request, Long currentUserId);

    ProjectDocVO setPrimaryDoc(Long docId, Long currentUserId);

    void deleteDoc(Long docId, Long currentUserId);

    List<ProjectDocVersionVO> listVersions(Long docId, Long currentUserId);

    ProjectDocVersionVO getVersion(Long docId, Integer versionNo, Long currentUserId);

    ProjectDocVersionCompareVO compareVersions(Long docId, Integer fromVersionNo, Integer toVersionNo, Long currentUserId);

    ProjectDocVO rollback(Long docId, Integer versionNo, Long currentUserId);
}
