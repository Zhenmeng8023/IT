function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function stripHtml(html) {
  const source = String(html || '')
  if (!source) return ''
  if (typeof window !== 'undefined' && window.document) {
    const container = window.document.createElement('div')
    container.innerHTML = source
    return normalizeText(container.textContent || container.innerText || '')
  }
  return normalizeText(source.replace(/<[^>]*>/g, ' '))
}

function uniqueList(list = []) {
  const seen = new Set()
  return (Array.isArray(list) ? list : [])
    .map(item => normalizeText(item))
    .filter(Boolean)
    .filter(item => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function normalizeTagId(value) {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  if (Number.isFinite(num) && num > 0) return num
  return null
}

function normalizeTagIds(rawTags = []) {
  const source = Array.isArray(rawTags) ? rawTags : []
  const ids = source
    .map(item => {
      if (item && typeof item === 'object') {
        return normalizeTagId(item.id || item.value)
      }
      return normalizeTagId(item)
    })
    .filter(Boolean)
  return Array.from(new Set(ids))
}

function buildTagNameMap(tagOptions = []) {
  const map = new Map()
  ;(Array.isArray(tagOptions) ? tagOptions : []).forEach(item => {
    const id = normalizeTagId(item && item.id)
    const name = normalizeText(item && item.name)
    if (!id || !name) return
    map.set(id, name)
  })
  return map
}

function resolveSelectedTagNames(rawTags = [], tagNameMap = new Map()) {
  if (!Array.isArray(rawTags) || !rawTags.length) return []
  const names = rawTags
    .map(item => {
      if (item && typeof item === 'object') {
        return normalizeText(item.name || item.label)
      }
      const id = normalizeTagId(item)
      return id ? normalizeText(tagNameMap.get(id)) : ''
    })
    .filter(Boolean)
  return uniqueList(names)
}

function resolveEditorHtml(blog = {}) {
  const html = blog && blog.content !== undefined ? blog.content : ''
  return String(html || '')
}

function resolveEditorText(quill, html) {
  if (quill && typeof quill.getText === 'function') {
    return normalizeText(quill.getText())
  }
  return stripHtml(html)
}

function clipText(value, max = 6000) {
  const text = normalizeText(value)
  if (!text) return ''
  return text.length > max ? text.slice(0, max) : text
}

function normalizeObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : null
}

function uniqueObjectTextList(list = [], mapper = item => item) {
  const seen = new Set()
  return (Array.isArray(list) ? list : [])
    .map(item => normalizeText(mapper(item)))
    .filter(Boolean)
    .filter(item => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

export function collectBlogWriteContext({ blog = {}, quill = null, tagOptions = [] } = {}) {
  const tagNameMap = buildTagNameMap(tagOptions)
  const contentHtml = resolveEditorHtml(blog)
  const contentText = resolveEditorText(quill, contentHtml)
  const tagIds = normalizeTagIds(blog.tags)
  const tagNames = resolveSelectedTagNames(blog.tags, tagNameMap)

  return {
    title: normalizeText(blog.title),
    content: contentText,
    contentText,
    contentHtml,
    editorContent: contentHtml,
    summary: normalizeText(blog.summary),
    tags: tagIds,
    tagIds,
    tagNames
  }
}

export function buildBlogWritePrompt(actionCode = '', contextPayload = {}) {
  const action = normalizeText(actionCode).toLowerCase()
  const title = normalizeText(contextPayload.title) || 'Untitled blog'
  const summary = normalizeText(contextPayload.summary)
  const tags = uniqueList(contextPayload.tagNames || contextPayload.tags || [])
  const contentText = clipText(contextPayload.contentText || contextPayload.content)

  if (action === 'blog.summary') {
    return [
      'Generate a concise blog summary and candidate tags for the current draft.',
      'Return JSON with fields: summary, tags, rejectTags.',
      'Requirements: summary <= 120 characters, tags should be specific and searchable.',
      '',
      `Title: ${title}`,
      summary ? `Current summary: ${summary}` : 'Current summary: (empty)',
      tags.length ? `Current tags: ${tags.join(', ')}` : 'Current tags: (empty)',
      '',
      'Draft content:',
      contentText || '(empty)'
    ].join('\n')
  }

  return [
    'Polish the blog draft content while preserving facts and intent.',
    'Return JSON with fields: polishedContent, changeSummary, warnings, titleSuggestions.',
    'Do not output analysis chatter outside the JSON.',
    '',
    `Title: ${title}`,
    summary ? `Current summary: ${summary}` : 'Current summary: (empty)',
    tags.length ? `Current tags: ${tags.join(', ')}` : 'Current tags: (empty)',
    '',
    'Draft content:',
    contentText || '(empty)'
  ].join('\n')
}

function resolveBlogDetailTags(rawTags = []) {
  return uniqueObjectTextList(
    Array.isArray(rawTags) ? rawTags : String(rawTags || '').split(','),
    item => item && typeof item === 'object' ? (item.name || item.label || item.value || item.tagName) : item
  ).slice(0, 10)
}

function resolveBlogDetailAccess(blog = {}) {
  const accessType = normalizeText(blog.accessType || blog.lockType || (blog.isVipOnly ? 'vip' : 'free')).toLowerCase() || 'free'
  const hasAccess = blog.hasAccess !== false
  const locked = blog.locked === true
  const canReadFullContent = hasAccess && !locked
  let visitStatus = '可访问全文'
  if (!canReadFullContent) {
    if (accessType === 'paid') {
      visitStatus = '当前仅可访问预览内容（付费内容未解锁）'
    } else if (accessType === 'vip') {
      visitStatus = '当前仅可访问预览内容（VIP 内容未解锁）'
    } else {
      visitStatus = '当前仅可访问预览内容'
    }
  }
  return {
    accessType,
    hasAccess,
    locked,
    canReadFullContent,
    visitStatus
  }
}

function normalizeBlogDetailComments(rawComments = []) {
  return (Array.isArray(rawComments) ? rawComments : [])
    .map(item => {
      const comment = normalizeObject(item) || {}
      const deleted = comment.deleted === true || normalizeText(comment.status).toLowerCase() === 'deleted'
      if (deleted) return null
      const content = clipText(comment.content || comment.body || comment.text, 120)
      if (!content) return null
      const author = normalizeText(
        comment.nickname ||
        comment.authorName ||
        comment.username ||
        (comment.author && (comment.author.displayName || comment.author.nickname || comment.author.username)) ||
        ''
      )
      return {
        author: author || '用户',
        content
      }
    })
    .filter(Boolean)
}

function buildBlogDetailCommentSummary(commentList = []) {
  const normalizedList = normalizeBlogDetailComments(commentList)
  if (!normalizedList.length) return '暂无评论'
  const lines = normalizedList.slice(0, 3).map((item, index) => `${index + 1}. ${item.author}：${item.content}`)
  return [`共 ${normalizedList.length} 条有效评论。`, ...lines].join('\n')
}

export function collectBlogDetailContext({ blog = {}, comments = [] } = {}) {
  const access = resolveBlogDetailAccess(blog)
  const readableContent = access.canReadFullContent
    ? String((blog && blog.content) || '')
    : String((blog && blog.previewContent) || '')
  const contentText = clipText(stripHtml(readableContent), 6000)
  const tags = resolveBlogDetailTags(blog.tags)
  const commentSummary = buildBlogDetailCommentSummary(comments)

  return {
    blogId: blog.id || null,
    title: normalizeText(blog.title) || '未命名博客',
    author: normalizeText(blog.author) || '未知作者',
    publishDate: normalizeText(blog.publishDate),
    tags,
    commentSummary,
    visitStatus: access.visitStatus,
    accessType: access.accessType,
    canReadFullContent: access.canReadFullContent,
    contentSource: access.canReadFullContent ? 'blog.content' : 'blog.previewContent',
    contentText
  }
}

export function buildBlogDetailPrompt(actionCode = '', contextPayload = {}) {
  const action = normalizeText(actionCode).toLowerCase()
  const title = normalizeText(contextPayload.title) || '未命名博客'
  const author = normalizeText(contextPayload.author) || '未知作者'
  const publishDate = normalizeText(contextPayload.publishDate) || '未知发布时间'
  const tags = uniqueList(contextPayload.tags || [])
  const visitStatus = normalizeText(contextPayload.visitStatus) || '未知访问状态'
  const commentSummary = clipText(contextPayload.commentSummary, 1200) || '暂无评论'
  const contentText = clipText(contextPayload.contentText, 6000) || '(empty)'

  const contextLines = [
    `标题：${title}`,
    `作者：${author}`,
    `发布时间：${publishDate}`,
    `标签：${tags.length ? tags.join('、') : '无'}`,
    `访问状态：${visitStatus}`,
    '评论摘要：',
    commentSummary,
    '博客内容：',
    contentText
  ]

  if (action === 'blog.detail.explain') {
    return [
      '请用更易懂的方式解释这篇博客，保持原意，不编造事实。',
      '输出 Markdown，包含：核心观点、概念解释、阅读建议。',
      '',
      ...contextLines
    ].join('\n')
  }

  if (action === 'blog.detail.possible-questions') {
    return [
      '请结合博客内容和评论摘要，猜测读者最可能疑惑的问题。',
      '输出 Markdown，给出 5-8 个问题，每个问题附 1-2 句简洁回答。',
      '',
      ...contextLines
    ].join('\n')
  }

  return [
    '请提炼当前博客详情的关键信息。',
    '输出 Markdown，包含：一句话总结、3-5 条核心信息、目标读者。',
    '',
    ...contextLines
  ].join('\n')
}

function normalizeTaskStatus(value) {
  const status = normalizeText(value).toLowerCase()
  if (!status) return '待处理'
  if (status === 'todo') return '待处理'
  if (status === 'in_progress') return '进行中'
  if (status === 'done') return '已完成'
  return status
}

function normalizeTaskPriority(value) {
  const priority = normalizeText(value).toUpperCase()
  if (!priority) return ''
  if (['P0', 'P1', 'P2', 'P3'].includes(priority)) return priority
  return priority
}

function normalizeProjectTaskList(taskList = []) {
  return (Array.isArray(taskList) ? taskList : [])
    .map(item => {
      const task = normalizeObject(item) || {}
      const title = normalizeText(task.title || task.taskName || task.name)
      if (!title) return null
      return {
        title,
        summary: normalizeText(task.description || task.content || task.remark),
        status: normalizeTaskStatus(task.statusName || task.status || task.state),
        priority: normalizeTaskPriority(task.priority || task.priorityCode || task.level),
        assigneeName: normalizeText(task.assigneeName || task.ownerName || task.executorName),
        dueDate: normalizeText(task.dueDate || task.deadline || task.endTime)
      }
    })
    .filter(Boolean)
    .slice(0, 12)
}

function buildProjectTaskSummary(taskList = []) {
  const list = normalizeProjectTaskList(taskList)
  if (!list.length) return '暂无任务'
  const total = list.length
  const todo = list.filter(item => item.status === '待处理').length
  const doing = list.filter(item => item.status === '进行中').length
  const done = list.filter(item => item.status === '已完成').length
  return `共 ${total} 项任务，待处理 ${todo} 项，进行中 ${doing} 项，已完成 ${done} 项。`
}

export function collectProjectDetailContext({ project = {}, taskList = [] } = {}) {
  const normalizedTasks = normalizeProjectTaskList(
    Array.isArray(taskList) && taskList.length
      ? taskList
      : (Array.isArray(project.tasks) ? project.tasks : [])
  )
  const projectName = normalizeText(project.name || project.projectName || project.title || '未命名项目')
  const projectDescription = normalizeText(
    project.description || project.summary || project.introduction || project.content || project.readme || ''
  )
  const currentStage = normalizeText(
    project.currentStage || project.stage || project.phase || project.statusName || project.status || project.state || '未明确'
  )
  const tagNames = uniqueObjectTextList(
    Array.isArray(project.tags) ? project.tags : String(project.tags || '').split(','),
    item => item && typeof item === 'object' ? (item.name || item.label || item.value) : item
  ).slice(0, 8)

  return {
    projectId: project.id || null,
    projectName,
    projectDescription,
    currentStage,
    taskSummary: buildProjectTaskSummary(normalizedTasks),
    currentTasks: normalizedTasks,
    taskTitles: normalizedTasks.map(item => item.title),
    readmeLeadText: clipText(project.readme || '', 1200),
    tags: tagNames
  }
}

function formatProjectTaskForPrompt(task = {}) {
  const meta = [
    task.status ? `状态：${task.status}` : '',
    task.priority ? `优先级：${task.priority}` : '',
    task.assigneeName ? `负责人：${task.assigneeName}` : '',
    task.dueDate ? `截止：${task.dueDate}` : ''
  ].filter(Boolean)
  const summary = normalizeText(task.summary)
  const extra = [summary, meta.join('，')].filter(Boolean).join('；')
  return extra ? `${task.title}（${extra}）` : task.title
}

export function buildProjectDetailPrompt(actionCode = '', contextPayload = {}) {
  const action = normalizeText(actionCode).toLowerCase()
  const projectName = normalizeText(contextPayload.projectName) || '未命名项目'
  const projectDescription = clipText(contextPayload.projectDescription, 1200) || '暂无项目简介'
  const currentStage = normalizeText(contextPayload.currentStage) || '未明确'
  const taskSummary = normalizeText(contextPayload.taskSummary) || '暂无任务概要'
  const taskLines = (Array.isArray(contextPayload.currentTasks) ? contextPayload.currentTasks : [])
    .slice(0, 8)
    .map(formatProjectTaskForPrompt)
  const taskBlock = taskLines.length ? taskLines.join('\n- ') : '暂无当前任务'

  if (action === 'project.detail.tasks') {
    return [
      '请基于当前项目上下文输出任务拆解草稿。',
      '只返回 JSON，不要输出解释文字。',
      'JSON 字段：phases, executionOrder, risks。',
      '其中 phases 为数组，每项包含 name, tasks；tasks 每项包含 title, goal, deliverable, priority, estimate。',
      '',
      `项目名称：${projectName}`,
      `项目简介：${projectDescription}`,
      `当前阶段：${currentStage}`,
      `任务概要：${taskSummary}`,
      '当前任务：',
      `- ${taskBlock}`
    ].join('\n')
  }

  if (action === 'project.detail.risks') {
    return [
      '请识别当前项目的主要风险。',
      '只返回 JSON，不要输出解释文字。',
      'JSON 字段：overview, risks。',
      'risks 为数组，每项可为字符串，或对象 { title, level, impact, mitigation }。',
      '',
      `项目名称：${projectName}`,
      `项目简介：${projectDescription}`,
      `当前阶段：${currentStage}`,
      `任务概要：${taskSummary}`,
      '当前任务：',
      `- ${taskBlock}`
    ].join('\n')
  }

  if (action === 'project.detail.next-steps') {
    return [
      '请给出当前项目最值得推进的下一步建议。',
      '只返回 JSON，不要输出解释文字。',
      'JSON 字段：overview, nextSteps, milestones。',
      'nextSteps 为数组，每项可为字符串，或对象 { title, owner, timeframe, expectedOutcome }。',
      'milestones 为字符串数组。',
      '',
      `项目名称：${projectName}`,
      `项目简介：${projectDescription}`,
      `当前阶段：${currentStage}`,
      `任务概要：${taskSummary}`,
      '当前任务：',
      `- ${taskBlock}`
    ].join('\n')
  }

  return [
    '请基于当前项目上下文生成项目概览草稿。',
    '只返回 JSON，不要输出解释文字。',
    'JSON 字段：overview, scenarios, features, risks, nextActions。',
    '',
    `项目名称：${projectName}`,
    `项目简介：${projectDescription}`,
    `当前阶段：${currentStage}`,
    `任务概要：${taskSummary}`,
    '当前任务：',
    `- ${taskBlock}`
  ].join('\n')
}

export function summarizeAiContextPayload(payload = {}) {
  if (!payload || typeof payload !== 'object') return '无上下文'
  const entries = Object.keys(payload).slice(0, 8).map(key => {
    const value = payload[key]
    if (Array.isArray(value)) {
      const preview = value.slice(0, 3).map(item => {
        if (item && typeof item === 'object') {
          return normalizeText(item.title || item.name || item.label || item.value)
        }
        return normalizeText(item)
      }).filter(Boolean)
      return `${key}: ${preview.join(' / ') || `数组(${value.length})`}`
    }
    if (value && typeof value === 'object') {
      const nestedKeys = Object.keys(value).slice(0, 3).join(', ')
      return `${key}: {${nestedKeys || '...'}}`
    }
    return `${key}: ${clipText(value, 120) || '-'}`
  })
  return entries.join('\n') || '无上下文'
}
