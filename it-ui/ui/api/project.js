import request from '@/utils/request'

export function createProject(data) {
  return request.post('/api/project', data)
}

export function updateProject(id, data) {
  return request.put(`/api/project/${id}`, data)
}

export function getProjectDetail(id) {
  return request.get(`/api/project/${id}`)
}

export function getProjectPage(params) {
  return request.get('/api/project/page', { params })
}

export function getMyProjects(params) {
  return request.get('/api/project/my', { params })
}

export function deleteProject(id) {
  return request.delete(`/api/project/${id}`)
}

export function getProjectMembers(projectId) {
  return request.get('/api/project/member/list', { params: { projectId } })
}

export function addProjectMember(data) {
  return request.post('/api/project/member', data)
}

export function updateProjectMemberRole(data) {
  return request.put('/api/project/member/role', data)
}

export function deleteProjectMember(memberId) {
  return request.delete(`/api/project/member/${memberId}`)
}

export function getProjectTasks(projectId) {
  return request.get('/api/project/task/list', { params: { projectId } })
}

export function createProjectTask(data) {
  return request.post('/api/project/task', data)
}

export function updateProjectTask(taskId, data) {
  return request.put(`/api/project/task/${taskId}`, data)
}

export function updateProjectTaskStatus(taskId, data) {
  return request.put(`/api/project/task/${taskId}/status`, data)
}

export function deleteProjectTask(taskId) {
  return request.delete(`/api/project/task/${taskId}`)
}

export function uploadProjectFile(formData) {
  return request.post('/api/project/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadProjectFileVersion(fileId, formData) {
  return request.post(`/api/project/file/${fileId}/version`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getProjectFiles(projectId) {
  return request.get('/api/project/file/list', { params: { projectId } })
}

export function getProjectFileVersions(fileId) {
  return request.get(`/api/project/file/${fileId}/versions`)
}

export function downloadProjectFile(fileId) {
  return request.get(`/api/project/file/download/${fileId}`, {
    responseType: 'blob'
  })
}
