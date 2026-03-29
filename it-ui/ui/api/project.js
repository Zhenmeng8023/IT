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

export async function getProjectStarStatus(projectId, options = {}) {
  const targetId = Number(projectId)
  if (!targetId) {
    return { projectId: targetId || projectId, starred: false, stars: 0 }
  }

  const pageSize = Number(options.pageSize) > 0 ? Number(options.pageSize) : 50
  const maxPages = Number(options.maxPages) > 0 ? Number(options.maxPages) : 20
  let page = 1
  let stars = 0

  while (page <= maxPages) {
    const response = await getMyStarredProjects({ page, size: pageSize })
    const pageData = response?.data || {}
    const list = Array.isArray(pageData.list) ? pageData.list : []

    if (page === 1) {
      const matched = list.find(item => Number(item.id) === targetId)
      stars = matched?.stars ?? 0
    }

    if (list.some(item => Number(item.id) === targetId)) {
      const matched = list.find(item => Number(item.id) === targetId)
      return {
        projectId: targetId,
        starred: true,
        stars: matched?.stars ?? stars ?? 0
      }
    }

    const total = Number(pageData.total) || 0
    if (!list.length || page * pageSize >= total) break
    page += 1
  }

  return { projectId: targetId, starred: false, stars: stars || 0 }
}

// ----------------- 成员 -----------------
export function listProjectMembers(projectId) {
  return request({ url: '/project/member/list', method: 'get', params: { projectId } })
}

export function searchProjectMemberUsers(keyword, size = 10) {
  return request({ url: '/project/member/user/search', method: 'get', params: { keyword, size } })
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
    url: '/project/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadProjectFiles(projectId, formData) {
  return request({
    url: '/project/file/upload/batch',
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

export function previewProjectFile(fileId) {
  return request({ url: `/project/file/preview/${fileId}`, method: 'get', responseType: 'blob' })
}

export function downloadFile(fileId) {
  return request({ url: `/project/file/download/${fileId}`, method: 'get', responseType: 'blob' })
}

export function downloadProjectFiles(projectId, fileIds = []) {
  return request({
    url: '/project/file/download/batch',
    method: 'post',
    data: { projectId, fileIds },
    responseType: 'blob'
  })
}
