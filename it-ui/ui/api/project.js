import request from '@/utils/request'

function normalizeUploadFile(file) {
  if (!file) return null
  if (file instanceof File) return file
  if (file.raw instanceof File) return file.raw
  return file
}

export const pageProjects = (params) => {
  return request({
    url: '/project/page',
    method: 'get',
    params
  }).then(response => {
    if (response) {
      return response
    }
    return { data: { list: [], total: 0, page: 1, size: 10 } }
  })
}

export function getProjectDetail(projectId) {
  return request({
    url: `/project/${projectId}`,
    method: 'get'
  })
}

export function getProjectContributors(projectId) {
  return request({
    url: `/project/${projectId}/contributors`,
    method: 'get'
  })
}

export function getRelatedProjects(projectId, params = {}) {
  return request({
    url: `/project/${projectId}/related`,
    method: 'get',
    params
  })
}

export function createProject(data) {
  return request({
    url: '/project',
    method: 'post',
    data
  })
}

export function updateProject(projectId, data) {
  return request({
    url: `/project/${projectId}`,
    method: 'put',
    data
  })
}

export function deleteProject(projectId) {
  return request({
    url: `/project/${projectId}`,
    method: 'delete'
  })
}

export function getMyProjects(params = {}) {
  return request({
    url: '/project/my',
    method: 'get',
    params
  })
}

export function getParticipatedProjects(params = {}) {
  return request({
    url: '/project/participated',
    method: 'get',
    params
  })
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
      return { projectId: targetId, starred: true, stars: matched?.stars ?? stars ?? 0 }
    }
    const total = Number(pageData.total) || 0
    if (!list.length || page * pageSize >= total) break
    page += 1
  }
  return { projectId: targetId, starred: false, stars: stars || 0 }
}

export function getMyStarredProjects(params = {}) {
  return request({
    url: '/project/star/my',
    method: 'get',
    params
  })
}

export function listProjectMembers(projectId) {
  return request({
    url: '/project/member/list',
    method: 'get',
    params: { projectId }
  })
}

export function searchProjectMemberUsers(keyword, size = 10) {
  return request({
    url: '/project/member/user/search',
    method: 'get',
    params: { keyword, size }
  })
}

export function addProjectMember(data) {
  return request({
    url: '/project/member',
    method: 'post',
    data
  })
}

export function updateProjectMemberRole(data) {
  return request({
    url: '/project/member/role',
    method: 'put',
    data
  })
}

export function removeProjectMember(memberId) {
  return request({
    url: `/project/member/${memberId}`,
    method: 'delete'
  })
}

export function quitProject(projectId) {
  return request({
    url: '/project/member/quit',
    method: 'post',
    params: { projectId }
  })
}


export function getProjectMemberStatus(projectId) {
  return request({
    url: '/project/member/status',
    method: 'get',
    params: { projectId }
  })
}

export function listProjectTasks(projectId, params = {}) {
  return request({
    url: '/project/task/list',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function listMyTasks(projectId) {
  return request({
    url: '/project/task/my',
    method: 'get',
    params: { projectId }
  })
}

export function createTask(data) {
  return request({
    url: '/project/task',
    method: 'post',
    data
  })
}

export function updateTask(taskId, data) {
  return request({
    url: `/project/task/${taskId}`,
    method: 'put',
    data
  })
}

export function updateTaskStatus(taskId, data) {
  return request({
    url: `/project/task/${taskId}/status`,
    method: 'put',
    data
  })
}

export function deleteTask(taskId) {
  return request({
    url: `/project/task/${taskId}`,
    method: 'delete'
  })
}

export function getTaskComments(taskId) {
  return request({
    url: `/project/task/${taskId}/comments`,
    method: 'get'
  })
}

export function createTaskComment(taskId, data) {
  return request({
    url: `/project/task/${taskId}/comments`,
    method: 'post',
    data
  })
}

export function replyTaskComment(commentId, data) {
  return request({
    url: `/project/task/comments/${commentId}/reply`,
    method: 'post',
    data
  })
}

export function deleteTaskComment(commentId) {
  return request({
    url: `/project/task/comments/${commentId}`,
    method: 'delete'
  })
}

export function getTaskChecklist(taskId) {
  return request({
    url: `/project/task/${taskId}/checklist`,
    method: 'get'
  })
}

export function createTaskChecklistItem(taskId, data) {
  return request({
    url: `/project/task/${taskId}/checklist`,
    method: 'post',
    data
  })
}

export function updateTaskChecklistItem(itemId, data) {
  return request({
    url: `/project/task/checklist/${itemId}`,
    method: 'put',
    data
  })
}

export function toggleTaskChecklistItem(itemId) {
  return request({
    url: `/project/task/checklist/${itemId}/toggle`,
    method: 'put'
  })
}

export function deleteTaskChecklistItem(itemId) {
  return request({
    url: `/project/task/checklist/${itemId}`,
    method: 'delete'
  })
}

export function sortTaskChecklistItems(taskId, data) {
  return request({
    url: `/project/task/${taskId}/checklist/sort`,
    method: 'put',
    data
  })
}

export function getTaskAttachments(taskId) {
  return request({
    url: `/project/task/${taskId}/attachments`,
    method: 'get'
  })
}

export function uploadTaskAttachment(taskId, fileOrFormData) {
  let formData = null
  if (fileOrFormData instanceof FormData) {
    formData = fileOrFormData
  } else {
    const rawFile = normalizeUploadFile(fileOrFormData)
    formData = new FormData()
    if (rawFile) {
      formData.append('file', rawFile, rawFile.name || 'attachment')
    }
  }
  return request({
    url: `/project/task/${taskId}/attachments/upload`,
    method: 'post',
    data: formData
  })
}

export function deleteTaskAttachment(attachmentId) {
  return request({
    url: `/project/task/attachments/${attachmentId}`,
    method: 'delete'
  })
}

export function downloadTaskAttachment(attachmentId) {
  return request({
    url: `/project/task/attachments/${attachmentId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

export function previewTaskAttachment(attachmentId) {
  return request({
    url: `/project/task/attachments/${attachmentId}/preview`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getTaskDependencies(taskId) {
  return request({
    url: `/project/task/${taskId}/dependencies`,
    method: 'get'
  })
}

export function createTaskDependency(taskId, data) {
  return request({
    url: `/project/task/${taskId}/dependencies`,
    method: 'post',
    data
  })
}

export function deleteTaskDependency(dependencyId) {
  return request({
    url: `/project/task/dependencies/${dependencyId}`,
    method: 'delete'
  })
}

export function getTaskLogs(taskId, params = {}) {
  return request({
    url: `/project/task/${taskId}/logs`,
    method: 'get',
    params
  })
}

export function listProjectFiles(projectId, branchId) {
  const params = { projectId }
  if (branchId !== undefined && branchId !== null && branchId !== '') {
    params.branchId = branchId
  }
  return request({
    url: '/project/file/list',
    method: 'get',
    params
  })
}

export function listFileVersions(fileId, branchId) {
  const params = {}
  if (branchId !== undefined && branchId !== null && branchId !== '') {
    params.branchId = branchId
  }
  return request({
    url: `/project/file/${fileId}/versions`,
    method: 'get',
    params
  })
}

export function uploadProjectFile(projectId, formData, branchId) {
  if (projectId !== undefined && projectId !== null && formData instanceof FormData && !formData.has('projectId')) {
    formData.append('projectId', String(projectId))
  }
  if (branchId !== undefined && branchId !== null && branchId !== '' && formData instanceof FormData && !formData.has('branchId')) {
    formData.append('branchId', String(branchId))
  }
  return request({
    url: '/project/file/upload',
    method: 'post',
    data: formData
  })
}

export function uploadProjectZip(projectId, fileOrFormData, extra = {}) {
  let formData = null
  if (fileOrFormData instanceof FormData) {
    formData = fileOrFormData
    if (projectId !== undefined && projectId !== null && !formData.has('projectId')) {
      formData.append('projectId', String(projectId))
    }
    if (extra.branchId !== undefined && extra.branchId !== null && extra.branchId !== '' && !formData.has('branchId')) {
      formData.append('branchId', String(extra.branchId))
    }
  } else {
    const rawFile = normalizeUploadFile(fileOrFormData)
    formData = new FormData()
    if (projectId !== undefined && projectId !== null) {
      formData.append('projectId', String(projectId))
    }
    if (extra.branchId !== undefined && extra.branchId !== null && extra.branchId !== '') {
      formData.append('branchId', String(extra.branchId))
    }
    if (rawFile) {
      formData.append('file', rawFile, rawFile.name || 'project.zip')
    }
    if (extra.version) {
      formData.append('version', extra.version)
    }
    if (extra.commitMessage) {
      formData.append('commitMessage', extra.commitMessage)
    }
  }
  return request({
    url: '/project/file/upload/zip',
    method: 'post',
    data: formData,
    timeout: 600000
  })
}

export function uploadProjectFiles(projectId, formData, branchId) {
  if (projectId !== undefined && projectId !== null && formData instanceof FormData && !formData.has('projectId')) {
    formData.append('projectId', String(projectId))
  }
  if (branchId !== undefined && branchId !== null && branchId !== '' && formData instanceof FormData && !formData.has('branchId')) {
    formData.append('branchId', String(branchId))
  }
  return request({
    url: '/project/file/upload/batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  })
}

export function uploadFileNewVersion(fileId, formData, branchId) {
  if (branchId !== undefined && branchId !== null && branchId !== '' && formData instanceof FormData && !formData.has('branchId')) {
    formData.append('branchId', String(branchId))
  }
  return request({
    url: `/project/file/${fileId}/version`,
    method: 'post',
    data: formData
  })
}

export function setMainFile(fileId, branchId) {
  const params = {}
  if (branchId !== undefined && branchId !== null && branchId !== '') {
    params.branchId = branchId
  }
  return request({
    url: `/project/file/${fileId}/main`,
    method: 'put',
    params
  })
}

export function deleteFile(fileId, branchId) {
  return request({
    url: `/project/file/${fileId}`,
    method: 'delete',
    params: branchId ? { branchId } : undefined
  })
}

export function previewProjectFile(fileId, branchId) {
  const params = {}
  if (branchId !== undefined && branchId !== null && branchId !== '') {
    params.branchId = branchId
  }
  return request({
    url: `/project/file/preview/${fileId}`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function downloadFile(fileId, branchId) {
  const params = {}
  if (branchId !== undefined && branchId !== null && branchId !== '') {
    params.branchId = branchId
  }
  return request({
    url: `/project/file/download/${fileId}`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function downloadProjectFiles(projectId, fileIds = []) {
  return request({
    url: '/project/file/download/batch',
    method: 'post',
    data: { projectId, fileIds },
    responseType: 'blob'
  })
}

export function unstageWorkspacePath(projectId, branchId, canonicalPath) {
  return request({
    url: '/project/workspace/unstage-path',
    method: 'post',
    params: { projectId, branchId, canonicalPath }
  })
}

export function discardWorkspacePath(projectId, branchId, canonicalPath) {
  return request({
    url: '/project/workspace/discard-path',
    method: 'post',
    params: { projectId, branchId, canonicalPath }
  })
}

export function resetWorkspace(projectId, branchId) {
  return request({
    url: '/project/workspace/reset',
    method: 'post',
    params: { projectId, branchId }
  })
}

export function discardWorkspace(projectId, branchId) {
  return request({
    url: '/project/workspace/discard-workspace',
    method: 'post',
    params: { projectId, branchId }
  })
}

export function listTaskReopenRequests(taskId) {
  return request({ url: `/project/task/${taskId}/reopen-requests`, method: 'get' })
}

export function applyTaskReopenRequest(taskId, data) {
  return request({ url: `/project/task/${taskId}/reopen-requests`, method: 'post', data })
}

export function approveTaskReopenRequest(taskId, requestId, data) {
  return request({ url: `/project/task/${taskId}/reopen-requests/${requestId}/approve`, method: 'post', data })
}

export function rejectTaskReopenRequest(taskId, requestId, data) {
  return request({ url: `/project/task/${taskId}/reopen-requests/${requestId}/reject`, method: 'post', data })
}

export function submitProjectJoinRequest(projectId, data) {
  return request({ url: `/project/${projectId}/join-requests`, method: 'post', data })
}

export function listProjectJoinRequests(projectId) {
  return request({ url: `/project/${projectId}/join-requests`, method: 'get' })
}

export function getMyProjectJoinRequestStatus(projectId) {
  return request({ url: `/project/${projectId}/join-requests/my-status`, method: 'get' })
}

export function auditProjectJoinRequest(requestId, data) {
  return request({ url: `/project/join-requests/${requestId}/audit`, method: 'put', data })
}

export function cancelProjectJoinRequest(requestId) {
  return request({ url: `/project/join-requests/${requestId}/cancel`, method: 'post' })
}

export function createProjectInvitation(data) {
  return request({ url: '/project/invitations', method: 'post', data })
}

export function listProjectInvitations(projectId, params = {}) {
  return request({ url: `/project/invitations/project/${projectId}`, method: 'get', params })
}

export function listMyPendingProjectInvitations() {
  return request({ url: '/project/invitations/my/pending', method: 'get' })
}

export function acceptProjectInvitation(invitationId) {
  return request({ url: `/project/invitations/${invitationId}/accept`, method: 'post' })
}

export function rejectProjectInvitation(invitationId) {
  return request({ url: `/project/invitations/${invitationId}/reject`, method: 'post' })
}

export function cancelProjectInvitation(invitationId) {
  return request({ url: `/project/invitations/${invitationId}/cancel`, method: 'post' })
}

