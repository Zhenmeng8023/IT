function parsePayload(item) {
  if (!item) return {}
  if (item.payload && typeof item.payload === 'object') return item.payload
  const raw = item.payloadJson || item.payload
  if (!raw || typeof raw !== 'string') return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    return {}
  }
}

function validValue(value) {
  return value !== undefined &&
    value !== null &&
    value !== '' &&
    String(value).toLowerCase() !== 'undefined' &&
    String(value).toLowerCase() !== 'null'
}

function pickValue(item, payload, keys) {
  for (const key of keys) {
    if (item && validValue(item[key])) return item[key]
    if (payload && validValue(payload[key])) return payload[key]
  }
  return null
}

function normalizeId(value) {
  return validValue(value) ? String(value) : ''
}

function inferProjectId(item, payload) {
  const direct = pickValue(item, payload, ['projectId', 'project_id'])
  if (direct) return normalizeId(direct)
  if (item && item.targetType === 'project') return normalizeId(item.targetId)
  if (payload && payload.targetType === 'project') return normalizeId(payload.targetId)
  return ''
}

function inferBlogId(item, payload) {
  const direct = pickValue(item, payload, ['blogId', 'blog_id', 'postId', 'articleId'])
  if (direct) return normalizeId(direct)
  if (item && item.targetType === 'blog') return normalizeId(item.targetId)
  if (payload && payload.targetType === 'blog') return normalizeId(payload.targetId)
  return ''
}

function inferCommentId(item, payload) {
  const direct = pickValue(item, payload, ['commentId', 'comment_id', 'replyId'])
  if (direct) return normalizeId(direct)
  if (item && ['comment', 'reply'].includes(item.sourceType)) return normalizeId(item.sourceId)
  return ''
}

function inferInvitationId(item, payload) {
  const direct = pickValue(item, payload, ['invitationId', 'invitation_id'])
  if (direct) return normalizeId(direct)
  if (item && item.type === 'project_invitation') return normalizeId(item.sourceId)
  return ''
}

function inferRequestId(item, payload) {
  const direct = pickValue(item, payload, ['requestId', 'request_id', 'joinRequestId'])
  if (direct) return normalizeId(direct)
  if (item && item.type === 'project_join_request') return normalizeId(item.sourceId)
  return ''
}

function appendQuery(path, query) {
  const params = new URLSearchParams()
  Object.keys(query || {}).forEach(key => {
    if (validValue(query[key])) params.set(key, String(query[key]))
  })
  const qs = params.toString()
  return qs ? `${path}?${qs}` : path
}

function routeFromType(item, payload) {
  const type = item && item.type
  const projectId = inferProjectId(item, payload)
  const blogId = inferBlogId(item, payload)
  const commentId = inferCommentId(item, payload)

  if (blogId && ['comment', 'reply', 'like', 'collect'].includes(type)) {
    return appendQuery(`/blog/${blogId}`, commentId ? { commentId, highlight: 'true' } : {})
  }
  if (type === 'project_invitation') {
    return appendQuery('/myproject', {
      tab: 'invitations',
      projectId,
      invitationId: inferInvitationId(item, payload)
    })
  }
  if (type === 'project_join_request') {
    return appendQuery('/projectmanage', {
      projectId,
      tab: 'member-manage',
      requestId: inferRequestId(item, payload)
    })
  }
  if (projectId && [
    'project_invitation_accepted',
    'project_invitation_rejected',
    'project_join_approved',
    'project_join_rejected',
    'project_star'
  ].includes(type)) {
    return appendQuery('/projectdetail', { projectId })
  }
  return ''
}

function normalizeInternalUrl(rawUrl, item, payload) {
  const trimmed = String(rawUrl || '').trim()
  if (!trimmed) return ''
  if (/^https?:\/\//i.test(trimmed)) return trimmed

  let parsed
  try {
    parsed = new URL(trimmed.charAt(0) === '/' ? trimmed : `/${trimmed}`, 'http://notification.local')
  } catch (error) {
    return routeFromType(item, payload)
  }

  const query = {}
  parsed.searchParams.forEach((value, key) => {
    if (key !== 'id') query[key] = value
  })

  if (parsed.pathname === '/projectdetail') {
    const projectId = normalizeId(parsed.searchParams.get('projectId')) ||
      normalizeId(parsed.searchParams.get('id')) ||
      inferProjectId(item, payload)
    return projectId ? appendQuery('/projectdetail', { ...query, projectId }) : routeFromType(item, payload)
  }

  if (parsed.pathname === '/projectmanage') {
    const projectId = normalizeId(parsed.searchParams.get('projectId')) ||
      normalizeId(parsed.searchParams.get('id')) ||
      inferProjectId(item, payload)
    const tab = query.tab === 'joinRequests' ? 'member-manage' : (query.tab || 'overview')
    return projectId ? appendQuery('/projectmanage', { ...query, projectId, tab }) : routeFromType(item, payload)
  }

  const blogMatch = parsed.pathname.match(/^\/blog\/([^/]+)?$/)
  if (blogMatch) {
    const blogId = normalizeId(blogMatch[1]) || inferBlogId(item, payload)
    const commentId = normalizeId(parsed.searchParams.get('commentId')) || inferCommentId(item, payload)
    return blogId ? appendQuery(`/blog/${blogId}`, { ...query, commentId, highlight: commentId ? (query.highlight || 'true') : query.highlight }) : routeFromType(item, payload)
  }

  if (parsed.pathname === '/myproject') {
    return appendQuery('/myproject', query)
  }

  return `${parsed.pathname}${parsed.search}${parsed.hash}`
}

export function resolveNotificationTarget(item) {
  const payload = parsePayload(item)
  const fromUrl = normalizeInternalUrl(item && item.actionUrl, item, payload)
  return fromUrl || routeFromType(item, payload) || '/notifications'
}

export function navigateToNotificationTarget(vm, item) {
  const target = resolveNotificationTarget(item)
  if (/^https?:\/\//i.test(target)) {
    window.location.href = target
    return Promise.resolve()
  }
  if (!target || target === '/notifications') {
    if (vm && vm.$message) vm.$message.warning('这条通知缺少完整跳转信息，已打开通知中心')
  }
  if (!vm || !vm.$router) return Promise.resolve()
  return vm.$router.push(target).catch(error => {
    if (error && error.name === 'NavigationDuplicated') return
    throw error
  })
}
