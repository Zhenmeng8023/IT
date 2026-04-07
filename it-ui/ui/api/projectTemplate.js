import request from '@/utils/request'

export function listProjectTemplates(params = {}) {
  return request({
    url: '/project/templates',
    method: 'get',
    params
  })
}

export function getProjectTemplateDetail(templateId) {
  return request({
    url: `/project/templates/${templateId}`,
    method: 'get'
  })
}

export function getProjectTemplateSource(projectId) {
  return request({
    url: `/project/${projectId}/template-source`,
    method: 'get'
  })
}

export function createProjectTemplate(data) {
  return request({
    url: '/project/templates',
    method: 'post',
    data
  })
}

export function updateProjectTemplate(templateId, data) {
  return request({
    url: `/project/templates/${templateId}`,
    method: 'put',
    data
  })
}

export function deleteProjectTemplate(templateId) {
  return request({
    url: `/project/templates/${templateId}`,
    method: 'delete'
  })
}

export function applyProjectTemplate(templateId, data) {
  return request({
    url: `/project/templates/${templateId}/apply`,
    method: 'post',
    data
  })
}

export function saveProjectAsTemplate(projectId, data) {
  return request({
    url: `/project/${projectId}/save-as-template`,
    method: 'post',
    data
  })
}
