import {
  getProjectDetail,
  listProjectFiles,
  listProjectMembers,
  listProjectTasks
} from '@/api/project'
import { getProjectActivities } from '@/api/projectActivity'
import { getToken } from '@/utils/auth'

export const PROJECT_STATUS_LABEL_MAP = {
  draft: '草稿',
  pending: '待审核',
  published: '已发布',
  rejected: '已拒绝',
  archived: '已归档'
}

export function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (error) {}
    return tags.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

export function formatBackendDateTime(dateLike) {
  if (!dateLike) return undefined
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return typeof dateLike === 'string' ? dateLike : undefined
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.click()
  URL.revokeObjectURL(url)
}

function decodeJwtPayload(token) {
  try {
    const parts = String(token || '').split('.')
    if (parts.length < 2) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    if (typeof window !== 'undefined' && typeof window.atob === 'function') {
      return JSON.parse(decodeURIComponent(escape(window.atob(padded))))
    }
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf-8'))
  } catch (error) {
    return null
  }
}

function pickUserIdFromObject(source) {
  if (!source || typeof source !== 'object') return null
  const keys = ['id', 'userId', 'uid', 'memberId', 'sub']
  for (const key of keys) {
    const value = source[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') return value
  }
  return null
}

export function readCurrentUserId() {
  if (!process.client) return null
  const storageKeys = ['userInfo', 'user', 'loginUser', 'currentUser', 'Admin-User', 'auth_user', 'authUser', 'memberInfo']
  for (const storage of [window.localStorage, window.sessionStorage]) {
    for (const key of storageKeys) {
      try {
        const raw = storage.getItem(key)
        if (!raw) continue
        const parsed = JSON.parse(raw)
        const foundId = pickUserIdFromObject(parsed)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') return Number(foundId)
      } catch (error) {}
    }
  }
  const token = getToken ? getToken() : ''
  if (token) {
    const payload = decodeJwtPayload(token)
    const foundId = pickUserIdFromObject(payload)
    if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') return Number(foundId)
  }
  return null
}

export function sameId(a, b) {
  if (a === null || a === undefined || b === null || b === undefined) return false
  return Number(a) === Number(b)
}

export function normalizeProject(apiData = {}) {
  return {
    id: apiData.id,
    title: apiData.name,
    description: apiData.description || '暂无项目描述',
    category: apiData.category || '',
    status: apiData.status || 'draft',
    statusText: PROJECT_STATUS_LABEL_MAP[apiData.status] || apiData.status || '-',
    visibility: apiData.visibility || 'public',
    tags: parseTags(apiData.tags),
    updateTime: apiData.updatedAt,
    authorId: apiData.authorId,
    authorName: apiData.authorName || '项目所有者',
    authorAvatar: apiData.authorAvatar || '',
    stars: apiData.stars || 0,
    downloads: apiData.downloads || 0,
    views: apiData.views || 0
  }
}

export function normalizeTask(task = {}) {
  return {
    id: task.id,
    title: task.title,
    description: task.description || '',
    status: task.status || 'todo',
    priority: task.priority || 'medium',
    assigneeId: task.assigneeId,
    assigneeName: task.assigneeName || '未分配',
    assigneeAvatar: task.assigneeAvatar || '',
    creatorName: task.creatorName || '',
    dueDate: task.dueDate,
    createdAt: task.createdAt,
    updatedAt: task.updatedAt,
    completedAt: task.completedAt,
    completedBy: task.completedBy,
    completedMemberJoinedAt: task.completedMemberJoinedAt,
    hasPendingReopenRequest: !!task.hasPendingReopenRequest,
    pendingReopenRequestId: task.pendingReopenRequestId,
    pendingReopenRequestedAt: task.pendingReopenRequestedAt,
    pendingReopenTargetStatus: task.pendingReopenTargetStatus
  }
}

export function normalizeMember(member = {}) {
  return {
    id: member.id,
    memberId: member.id,
    userId: member.userId,
    name: member.nickname || member.username || `用户${member.userId}`,
    username: member.username || `用户${member.userId}`,
    nickname: member.nickname || '',
    avatar: member.avatar || '',
    role: member.role || 'member',
    joinTime: member.joinedAt,
    joinedAt: member.joinedAt,
    isOwner: false
  }
}

export function buildOwnerRow(project = {}) {
  if (!project.authorId) return null
  return {
    id: `owner-${project.authorId}`,
    memberId: null,
    userId: project.authorId,
    name: project.authorName || '项目所有者',
    username: project.authorName || `用户${project.authorId}`,
    nickname: project.authorName || '项目所有者',
    avatar: project.authorAvatar || '',
    role: 'owner',
    joinTime: project.updateTime,
    joinedAt: '',
    isOwner: true
  }
}

export function mergeMembersWithOwner(project, members = []) {
  const owner = buildOwnerRow(project)
  const ownerUserId = owner ? Number(owner.userId) : null
  const memberRows = members
    .map(normalizeMember)
    .filter(item => {
      if (item.role === 'owner') return false
      if (!ownerUserId) return true
      return Number(item.userId) !== ownerUserId
    })
  return [owner, ...memberRows].filter(Boolean)
}

export function normalizeFile(file = {}) {
  return {
    id: file.id,
    fileName: file.fileName,
    filePath: file.filePath,
    fileSizeBytes: file.fileSizeBytes,
    fileType: file.fileType,
    uploadTime: file.uploadTime,
    isMain: !!file.isMain,
    version: file.version || '',
    versions: file.versions || []
  }
}

export function getProjectStatusType(status) {
  return { draft: 'info', pending: 'warning', published: 'success', rejected: 'danger', archived: 'info' }[status] || 'info'
}

export function getTaskStatusType(status) {
  return { todo: 'info', in_progress: 'warning', done: 'success' }[status] || 'info'
}

export function getTaskStatusText(status) {
  return { todo: '待处理', in_progress: '进行中', done: '已完成' }[status] || status
}

export function getTaskPriorityType(priority) {
  return { low: 'info', medium: '', high: 'warning', urgent: 'danger' }[priority] || ''
}

export function getTaskPriorityText(priority) {
  return { low: '低', medium: '中', high: '高', urgent: '紧急' }[priority] || priority
}

export function getMemberRoleText(role) {
  return { owner: '所有者', admin: '管理员', member: '成员', viewer: '查看者' }[role] || role
}

export function getInvitationStatusText(status) {
  return { pending: '待接受', accepted: '已接受', rejected: '已拒绝', cancelled: '已撤销', expired: '已过期' }[status] || status || '-'
}

export function getInvitationStatusTag(status) {
  return { pending: 'warning', accepted: 'success', rejected: 'info', cancelled: 'info', expired: 'danger' }[status] || 'info'
}

export function getJoinRequestStatusText(status) {
  return { pending: '待处理', approved: '已通过', rejected: '已拒绝', cancelled: '已取消' }[status] || status || '-'
}

export function getJoinRequestStatusTag(status) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }[status] || 'info'
}

export function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) return timeStr
  return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

export function formatFileSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function normalizeActivityPayload(res, fallbackSize = 8) {
  const payload = res && res.data ? res.data : res
  if (payload && Array.isArray(payload.list)) {
    return {
      list: payload.list,
      total: Number(payload.total || 0)
    }
  }
  if (Array.isArray(payload)) {
    return {
      list: payload.slice(0, fallbackSize),
      total: payload.length
    }
  }
  return {
    list: [],
    total: 0
  }
}

export async function fetchProjectManageContext(projectId) {
  const [projectResponse, memberResponse] = await Promise.all([
    getProjectDetail(projectId),
    listProjectMembers(projectId)
  ])
  const project = normalizeProject(projectResponse.data || {})
  const members = mergeMembersWithOwner(project, memberResponse.data || [])
  return { project, members }
}

export async function fetchProjectManageSummary(projectId, { canSeeTaskCollaboration = false } = {}) {
  const requests = [
    canSeeTaskCollaboration ? listProjectTasks(projectId) : Promise.resolve({ data: [] }),
    listProjectFiles(projectId),
    getProjectActivities(projectId, { page: 1, size: 1 })
  ]
  const [taskResult, fileResult, activityResult] = await Promise.allSettled(requests)
  const taskCount = taskResult.status === 'fulfilled' ? (Array.isArray(taskResult.value.data) ? taskResult.value.data.length : 0) : 0
  const fileCount = fileResult.status === 'fulfilled' ? (Array.isArray(fileResult.value.data) ? fileResult.value.data.length : 0) : 0
  const activityTotal = activityResult.status === 'fulfilled' ? normalizeActivityPayload(activityResult.value, 1).total : 0
  return { taskCount, fileCount, activityTotal }
}
