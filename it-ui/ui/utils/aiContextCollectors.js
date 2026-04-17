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
