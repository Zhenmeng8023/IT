import request from '@/utils/request'

// ----------------- 项目 -----------------
export const pageProjects = (params) => {
  return request({ url: '/project/page', method: 'get', params }).then(response => {
    if (response) {
      return response
    } else {
      return { data: { list: [], total: 0, page: 1, size: 10 } }
    }
  })
}

export function getProjectDetail(projectId) {
  return request({ url: `/project/${projectId}`, method: 'get' })
}

export function getProjectContributors(projectId) {
  return request({ url: `/project/${projectId}/contributors`, method: 'get' })
}

export function getRelatedProjects(projectId, params = {}) {
  return request({ url: `/project/${projectId}/related`, method: 'get', params })
}

export function createProject(data) {
  return request({ url: '/project', method: 'post', data })
}

export function updateProject(projectId, data) {
  return request({ url: `/project/${projectId}`, method: 'put', data })
}

export function deleteProject(projectId) {
  return request({ url: `/project/${projectId}`, method: 'delete' })
}

export function getMyProjects(params = {}) {
  return request({ url: '/project/my', method: 'get', params })
}

export function getParticipatedProjects(params = {}) {
  return request({ url: '/project/participated', method: 'get', params })
}

export function starProject(projectId) {
  return request({
    url: '/project/star',
    method: 'post',
    params: { projectId }
  })
}

export function unstarProject(projectId) {
  return request({
    url: '/project/star',
    method: 'delete',
    params: { projectId }
  })
}

export function getMyStarredProjects(params = {}) {
  return request({
    url: '/project/star/my',
    method: 'get',
    params
  })
}

// ----------------- 成员 -----------------
export function listProjectMembers(projectId) {
  return request({ url: '/project/member/list', method: 'get', params: { projectId } })
}

export function addProjectMember(data) {
  return request({ url: '/project/member', method: 'post', data })
}

export function updateProjectMemberRole(data) {
  return request({ url: '/project/member/role', method: 'put', data })
}

export function removeProjectMember(memberId) {
  return request({ url: `/project/member/${memberId}`, method: 'delete' })
}

export function quitProject(projectId) {
  return request({ url: '/project/member/quit', method: 'post', params: { projectId } })
}

// ----------------- 任务 -----------------
export function listProjectTasks(projectId, params = {}) {
  return request({ url: '/project/task/list', method: 'get', params: { projectId, ...params } })
}

export function listMyTasks(projectId) {
  return request({ url: '/project/task/my', method: 'get', params: { projectId } })
}

export function createTask(data) {
  return request({ url: '/project/task', method: 'post', data })
}

export function updateTask(taskId, data) {
  return request({ url: `/project/task/${taskId}`, method: 'put', data })
}

export function updateTaskStatus(taskId, data) {
  return request({ url: `/project/task/${taskId}/status`, method: 'put', data })
}

export function deleteTask(taskId) {
  return request({ url: `/project/task/${taskId}`, method: 'delete' })
}

// ----------------- 文件 -----------------
export function listProjectFiles(projectId) {
  return request({ url: '/project/file/list', method: 'get', params: { projectId } })
}

export function listFileVersions(fileId) {
  return request({ url: `/project/file/${fileId}/versions`, method: 'get' })
}

export function uploadProjectFile(projectId, formData) {
  return request({
    url: `/project/file/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadFileNewVersion(fileId, formData) {
  return request({
    url: `/project/file/${fileId}/version`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function setMainFile(fileId) {
  return request({ url: `/project/file/${fileId}/main`, method: 'put' })
}

export function deleteFile(fileId) {
  return request({ url: `/project/file/${fileId}`, method: 'delete' })
}

export function downloadFile(fileId) {
  return request({ url: `/project/file/download/${fileId}`, method: 'get', responseType: 'blob' })
}
