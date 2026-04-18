import request from '@/utils/request'

export const GetOperationLogsPage = (params) => {
  return request({
    url: '/admin/logs/operations/page',
    method: 'get',
    params
  })
}

export const ExportOperationLogs = (params) => {
  return request({
    url: '/admin/logs/operations/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export const GetOperationLogDetail = (id) => {
  return request({
    url: `/admin/logs/operations/${id}`,
    method: 'get'
  })
}

export const CleanExpiredLogs = (data) => {
  return request({
    url: '/admin/logs/clean',
    method: 'delete',
    data
  })
}

