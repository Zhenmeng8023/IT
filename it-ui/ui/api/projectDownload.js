import request from '@/utils/request'

export function getProjectDownloadSummary(projectId) {
  return request({
    url: '/project/download-record/summary',
    method: 'get',
    params: { projectId }
  })
}

export function pageProjectDownloadRecords(projectId, params = {}) {
  return request({
    url: '/project/download-record/page',
    method: 'get',
    params: { projectId, ...params }
  })
}
