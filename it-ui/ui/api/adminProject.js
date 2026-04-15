import request from '@/utils/request'

function shouldFallback(error) {
  const status = error && error.response ? error.response.status : 0
  return status === 404 || status === 405 || status === 501
}

function normalizeIdList(ids) {
  if (!Array.isArray(ids)) return []
  return ids
    .map(item => Number(item))
    .filter(item => Number.isFinite(item) && item > 0)
}

function buildFallbackPageParams(params = {}) {
  const next = { ...params }
  next.status = 'archived'
  return next
}

export async function getArchivedProjectsPage(params) {
  try {
    return await request({
      url: '/admin/projects/archived/page',
      method: 'get',
      params
    })
  } catch (error) {
    if (!shouldFallback(error)) throw error
    return request({
      url: '/project/page',
      method: 'get',
      params: buildFallbackPageParams(params)
    })
  }
}

export async function restoreArchivedProject(projectId) {
  const id = Number(projectId)
  try {
    return await request({
      url: `/admin/projects/archived/${id}/restore`,
      method: 'put'
    })
  } catch (error) {
    if (!shouldFallback(error)) throw error
    return request({
      url: `/project/${id}`,
      method: 'put',
      data: { status: 'published' }
    })
  }
}

export async function deleteArchivedProject(projectId) {
  const id = Number(projectId)
  try {
    return await request({
      url: `/admin/projects/archived/${id}`,
      method: 'delete'
    })
  } catch (error) {
    if (!shouldFallback(error)) throw error
    return request({
      url: `/project/${id}`,
      method: 'delete'
    })
  }
}

export async function batchRestoreArchivedProjects(ids) {
  const list = normalizeIdList(ids)
  if (!list.length) return Promise.resolve({ data: [] })

  try {
    return await request({
      url: '/admin/projects/archived/batch/restore',
      method: 'put',
      data: { ids: list }
    })
  } catch (error) {
    if (!shouldFallback(error)) throw error
    return Promise.all(list.map(id => restoreArchivedProject(id)))
  }
}

export async function batchDeleteArchivedProjects(ids) {
  const list = normalizeIdList(ids)
  if (!list.length) return Promise.resolve({ data: [] })

  try {
    return await request({
      url: '/admin/projects/archived/batch/delete',
      method: 'delete',
      data: { ids: list }
    })
  } catch (error) {
    if (!shouldFallback(error)) throw error
    return Promise.all(list.map(id => deleteArchivedProject(id)))
  }
}
