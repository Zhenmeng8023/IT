import request from '@/utils/request'

export function getMyNotifications(params = {}) {
  return request({
    url: '/notifications/my',
    method: 'get',
    params
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

export function getUnreadNotificationCounts() {
  return request({
    url: '/notifications/unread-counts',
    method: 'get'
  })
}

export function markNotificationAsRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'put'
  })
}

export function markAllNotificationsAsRead(params = {}) {
  return request({
    url: '/notifications/read-all',
    method: 'put',
    params
  })
}

export function deleteNotification(id) {
  return request({
    url: `/notifications/${id}`,
    method: 'delete'
  })
}

export function clearNotifications() {
  return request({
    url: '/notifications/clear',
    method: 'delete'
  })
}

export function getAdminNotifications(params = {}) {
  return request({
    url: '/admin/notifications/page',
    method: 'get',
    params
  })
}

export function getAdminNotificationDetail(id) {
  return request({
    url: `/admin/notifications/${id}`,
    method: 'get'
  })
}

export function deleteAdminNotification(id) {
  return request({
    url: `/admin/notifications/${id}`,
    method: 'delete'
  })
}

export function batchDeleteAdminNotifications(ids = []) {
  return request({
    url: '/admin/notifications/batch',
    method: 'delete',
    data: { ids }
  })
}

export function markAdminNotificationAsRead(id) {
  return request({
    url: `/admin/notifications/${id}/read`,
    method: 'put'
  })
}

export function batchMarkAdminNotificationsAsRead(ids = []) {
  return request({
    url: '/admin/notifications/read-all',
    method: 'put',
    data: { ids }
  })
}

export function sendAdminSystemNotification(data) {
  return request({
    url: '/admin/notifications/system',
    method: 'post',
    data
  })
}

export function broadcastAdminNotification(data) {
  return request({
    url: '/admin/notifications/broadcast',
    method: 'post',
    data
  })
}

export function getAdminNotificationStats() {
  return request({
    url: '/admin/notifications/stats',
    method: 'get'
  })
}

export function searchNotificationUsers(params = {}) {
  return request({
    url: '/admin/notifications/users/search',
    method: 'get',
    params
  })
}

export function getNotificationTemplates(params = {}) {
  return request({
    url: '/admin/notification-templates/page',
    method: 'get',
    params
  })
}

export function createNotificationTemplate(data) {
  return request({
    url: '/admin/notification-templates',
    method: 'post',
    data
  })
}

export function updateNotificationTemplate(id, data) {
  return request({
    url: `/admin/notification-templates/${id}`,
    method: 'put',
    data
  })
}

export function updateNotificationTemplateEnabled(id, enabled) {
  return request({
    url: `/admin/notification-templates/${id}/enabled`,
    method: 'put',
    data: { enabled }
  })
}

export function deleteNotificationTemplate(id) {
  return request({
    url: `/admin/notification-templates/${id}`,
    method: 'delete'
  })
}
