package com.alikeyou.itmoduleproject.support;

public interface ProjectPermissionService {
    void assertProjectReadable(Long projectId, Long userId);

    void assertProjectWritable(Long projectId, Long userId);

    void assertProjectManageMembers(Long projectId, Long userId);

    void assertProjectOwner(Long projectId, Long userId);

    boolean canManageProject(Long projectId, Long userId);
}
