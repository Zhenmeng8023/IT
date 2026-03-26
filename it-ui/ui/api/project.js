import request from '@/utils/request'

// ----------------- 项目 -----------------
export function pageProjects(params) {
  return request({
    url: '/api/project/page',
    method: 'get',
    params
  })
}

export function getProjectDetail(projectId) {
  return request({
    url: `/api/project/${projectId}`,
    method: 'get'
  })
}

export function createProject(data) {
  return request({
    url: '/api/project',
    method: 'post',
    data
  })
}

export function updateProject(projectId, data) {
  return request({
    url: `/api/project/${projectId}`,
    method: 'put',
    data
  })
}

export function deleteProject(projectId) {
  return request({
    url: `/api/project/${projectId}`,
    method: 'delete'
  })
}

// ----------------- 成员 -----------------
export function listProjectMembers(projectId) {
  return request({
    url: '/api/project/member/list',
    method: 'get',
    params: { projectId }
  })
}

export function addProjectMember(data) {
  return request({
    url: '/api/project/member',
    method: 'post',
    data
  })
}

export function updateProjectMemberRole(data) {
  return request({
    url: '/api/project/member/role',
    method: 'put',
    data
  })
}

export function removeProjectMember(memberId) {
  return request({
    url: `/api/project/member/${memberId}`,
    method: 'delete'
  })
}

export function quitProject(projectId) {
  return request({
    url: '/api/project/member/quit',
    method: 'post',
    params: { projectId }
  })
}

// ----------------- 任务 -----------------
export function listProjectTasks(projectId, params = {}) {
  return request({
    url: '/api/project/task/list',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function listMyTasks(projectId) {
  return request({
    url: '/api/project/task/my',
    method: 'get',
    params: { projectId }
  })
}

export function createTask(data) {
  return request({
    url: '/api/project/task',
    method: 'post',
    data
  })
}

export function updateTask(taskId, data) {
  return request({
    url: `/api/project/task/${taskId}`,
    method: 'put',
    data
  })
}

export function updateTaskStatus(taskId, data) {
  return request({
    url: `/api/project/task/${taskId}/status`,
    method: 'put',
    data
  })
}

export function deleteTask(taskId) {
  return request({
    url: `/api/project/task/${taskId}`,
    method: 'delete'
  })
}

// ----------------- 文件 -----------------
export function listProjectFiles(projectId) {
  return request({
    url: '/api/project/file/list',
    method: 'get',
    params: { projectId }
  })
}

export function listFileVersions(fileId) {
  return request({
    url: `/api/project/file/${fileId}/versions`,
    method: 'get'
  })
}

export function uploadProjectFile(projectId, formData) {
  return request({
    url: `/api/project/file/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadFileNewVersion(fileId, formData) {
  return request({
    url: `/api/project/file/${fileId}/version`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function setMainFile(fileId) {
  return request({
    url: `/api/project/file/${fileId}/main`,
    method: 'put'
  })
}

export function deleteFile(fileId) {
  return request({
    url: `/api/project/file/${fileId}`,
    method: 'delete'
  })
}

export function downloadFile(fileId) {
  return request({
    url: `/api/project/file/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}