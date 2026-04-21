import axios from 'axios'
import { API_BASE_URL } from '@/utils/backend'

if (!axios.defaults.baseURL) {
  axios.defaults.baseURL = API_BASE_URL
}
axios.defaults.withCredentials = true

function normalizePagePayload(payload) {
  const source = payload || {}
  return {
    content: Array.isArray(source.content) ? source.content : [],
    totalElements: Number(source.totalElements || 0),
    totalPages: Number(source.totalPages || 0),
    number: Number(source.number || 0),
    size: Number(source.size || 10)
  }
}

async function get(url, params) {
  const response = await axios.get(url, { params })
  return response.data
}

async function post(url, data) {
  const response = await axios.post(url, data)
  return response.data
}

async function put(url, data) {
  const response = await axios.put(url, data)
  return response.data
}

async function del(url) {
  const response = await axios.delete(url)
  return response.data
}

export async function getAdminRolePage(params = {}) {
  const payload = await get('/api/admin/rbac/roles/page', params)
  return normalizePagePayload(payload)
}

export async function listAdminRoles(params = {}) {
  const payload = await get('/api/admin/rbac/roles', params)
  return Array.isArray(payload) ? payload : []
}

export async function getAdminRoleById(id) {
  return get(`/api/admin/rbac/roles/${id}`)
}

export async function createAdminRole(data) {
  return post('/api/admin/rbac/roles', data)
}

export async function updateAdminRole(id, data) {
  return put(`/api/admin/rbac/roles/${id}`, data)
}

export async function deleteAdminRole(id) {
  return del(`/api/admin/rbac/roles/${id}`)
}

export async function assignAdminRoleMenus(roleId, menuIds) {
  return put(`/api/admin/rbac/roles/${roleId}/menus`, { menuIds })
}

export async function getAdminRoleMenus(roleId) {
  const payload = await get(`/api/admin/rbac/roles/${roleId}/menus`)
  return Array.isArray(payload) ? payload : []
}

export async function assignAdminRolePermissions(roleId, permissionIds) {
  return put(`/api/admin/rbac/roles/${roleId}/permissions`, { permissionIds })
}

export async function getAdminRolePermissions(roleId) {
  const payload = await get(`/api/admin/rbac/roles/${roleId}/permissions`)
  return Array.isArray(payload) ? payload : []
}

export async function getAdminPermissionPage(params = {}) {
  const payload = await get('/api/admin/rbac/permissions/page', params)
  return normalizePagePayload(payload)
}

export async function listAdminPermissions(params = {}) {
  const payload = await get('/api/admin/rbac/permissions', params)
  return Array.isArray(payload) ? payload : []
}

export async function getAdminPermissionById(id) {
  return get(`/api/admin/rbac/permissions/${id}`)
}

export async function createAdminPermission(data) {
  return post('/api/admin/rbac/permissions', data)
}

export async function updateAdminPermission(id, data) {
  return put(`/api/admin/rbac/permissions/${id}`, data)
}

export async function deleteAdminPermission(id) {
  return del(`/api/admin/rbac/permissions/${id}`)
}

export async function getAdminMenuPage(params = {}) {
  const payload = await get('/api/admin/rbac/menus/page', params)
  return normalizePagePayload(payload)
}

export async function listAdminMenus(params = {}) {
  const payload = await get('/api/admin/rbac/menus', params)
  return Array.isArray(payload) ? payload : []
}

export async function getAdminMenuById(id) {
  return get(`/api/admin/rbac/menus/${id}`)
}

export async function createAdminMenu(data) {
  return post('/api/admin/rbac/menus', data)
}

export async function updateAdminMenu(id, data) {
  return put(`/api/admin/rbac/menus/${id}`, data)
}

export async function deleteAdminMenu(id) {
  return del(`/api/admin/rbac/menus/${id}`)
}
