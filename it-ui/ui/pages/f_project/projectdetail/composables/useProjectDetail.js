import hljs from 'highlight.js/lib/common'
import JSZip from 'jszip'

export const CATEGORY_MAP = {
  frontend: '前端项目',
  backend: '后端项目',
  fullstack: '全栈项目',
  mobile: '移动应用',
  ai: 'AI 项目',
  tools: '工具项目'
}

export const STATUS_MAP = {
  draft: '草稿',
  pending: '待审核',
  published: '已发布',
  rejected: '已拒绝',
  archived: '已归档'
}

export const STATUS_TAG_MAP = {
  draft: 'info',
  pending: 'warning',
  published: 'success',
  rejected: 'danger',
  archived: 'info'
}

export const ROLE_MAP = {
  owner: '创建者',
  admin: '管理员',
  member: '成员',
  viewer: '查看者'
}

export const CODE_EXTENSIONS = new Set([
  'js', 'mjs', 'cjs', 'jsx', 'ts', 'tsx', 'vue', 'json', 'html', 'htm', 'css', 'scss', 'less',
  'java', 'kt', 'xml', 'yml', 'yaml', 'sql', 'sh', 'bash', 'zsh', 'py', 'rb', 'go', 'rs', 'c',
  'cpp', 'cc', 'cxx', 'h', 'hpp', 'cs', 'php', 'ini', 'log', 'properties', 'txt'
])

export const MARKDOWN_EXTENSIONS = new Set(['md', 'markdown'])
export const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'bmp', 'svg', 'webp', 'ico', 'avif'])
export const PDF_EXTENSIONS = new Set(['pdf'])
export const AUDIO_EXTENSIONS = new Set(['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'])
export const VIDEO_EXTENSIONS = new Set(['mp4', 'webm', 'ogg', 'mov', 'm4v'])
export const TABLE_EXTENSIONS = new Set(['csv', 'tsv'])
export const DOCX_EXTENSIONS = new Set(['docx'])
export const SPREADSHEET_EXTENSIONS = new Set(['xlsx'])
export const PRESENTATION_EXTENSIONS = new Set(['pptx'])
export const LEGACY_OFFICE_EXTENSIONS = new Set(['doc', 'xls', 'ppt'])
export const TEXT_EXTENSIONS = new Set([
  ...Array.from(CODE_EXTENSIONS),
  ...Array.from(MARKDOWN_EXTENSIONS),
  ...Array.from(TABLE_EXTENSIONS)
])

export const HIGHLIGHT_LANGUAGE_MAP = {
  js: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  jsx: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  vue: 'xml',
  java: 'java',
  kt: 'kotlin',
  py: 'python',
  rb: 'ruby',
  go: 'go',
  rs: 'rust',
  c: 'c',
  h: 'c',
  cpp: 'cpp',
  cc: 'cpp',
  cxx: 'cpp',
  hpp: 'cpp',
  cs: 'csharp',
  php: 'php',
  json: 'json',
  yml: 'yaml',
  yaml: 'yaml',
  xml: 'xml',
  html: 'xml',
  htm: 'xml',
  css: 'css',
  scss: 'scss',
  less: 'less',
  md: 'markdown',
  sql: 'sql',
  sh: 'bash',
  bash: 'bash',
  zsh: 'bash',
  properties: 'properties',
  ini: 'ini',
  txt: 'plaintext',
  log: 'plaintext'
}

export function getHighlightLanguage(extension = '') {
  const ext = String(extension || '').trim().toLowerCase()
  const target = HIGHLIGHT_LANGUAGE_MAP[ext] || ext
  if (!target) return 'plaintext'
  return hljs.getLanguage(target) ? target : 'plaintext'
}

export function detectPreviewType(extension = '') {
  const ext = String(extension || '').trim().toLowerCase()
  if (CODE_EXTENSIONS.has(ext)) return 'code'
  if (MARKDOWN_EXTENSIONS.has(ext)) return 'markdown'
  if (TABLE_EXTENSIONS.has(ext)) return 'table'
  if (IMAGE_EXTENSIONS.has(ext)) return 'image'
  if (PDF_EXTENSIONS.has(ext)) return 'pdf'
  if (AUDIO_EXTENSIONS.has(ext)) return 'audio'
  if (VIDEO_EXTENSIONS.has(ext)) return 'video'
  if (DOCX_EXTENSIONS.has(ext)) return 'docx'
  if (SPREADSHEET_EXTENSIONS.has(ext)) return 'spreadsheet'
  if (PRESENTATION_EXTENSIONS.has(ext)) return 'presentation'
  if (LEGACY_OFFICE_EXTENSIONS.has(ext)) return 'office-legacy'
  if (TEXT_EXTENSIONS.has(ext)) return 'text'
  return 'binary'
}

export function createEmptyPreviewState() {
  return {
    id: null,
    name: '',
    path: '',
    size: 0,
    extension: '',
    actualType: '',
    content: '',
    isMain: false,
    versions: [],
    previewType: '',
    blobUrl: '',
    mimeType: '',
    markdownHtml: '',
    tablePreview: {
      headers: [],
      rows: []
    },
    officePreview: {
      paragraphs: [],
      sheets: [],
      slides: []
    },
    previewError: '',
    previewLoading: false
  }
}

export function normalizeLineBreaks(text = '') {
  return String(text || '').replace(/\r\n?/g, '\n')
}


export async function safeReadBlobText(blob) {
  if (!blob) return ''
  try {
    if (typeof blob.text === 'function') {
      const text = await blob.text()
      return normalizeLineBreaks(text)
    }
  } catch (error) {}

  try {
    const buffer = await blob.arrayBuffer()
    const utf8 = new TextDecoder('utf-8', { fatal: false }).decode(buffer)
    return normalizeLineBreaks(utf8)
  } catch (error) {}

  return ''
}

export async function blobLooksLikeZip(blob) {
  if (!blob) return false
  try {
    const buffer = await blob.slice(0, 4).arrayBuffer()
    const bytes = new Uint8Array(buffer)
    return bytes.length >= 4 && bytes[0] === 0x50 && bytes[1] === 0x4B && (
      (bytes[2] === 0x03 && bytes[3] === 0x04) ||
      (bytes[2] === 0x05 && bytes[3] === 0x06) ||
      (bytes[2] === 0x07 && bytes[3] === 0x08)
    )
  } catch (error) {
    return false
  }
}

export function parseDelimitedText(source = '', delimiter = ',') {
  const text = normalizeLineBreaks(String(source || '')).replace(/^\uFEFF/, '')
  const rows = []
  let currentRow = []
  let currentCell = ''
  let inQuotes = false

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i]
    const next = text[i + 1]
    if (char === '"') {
      if (inQuotes && next === '"') {
        currentCell += '"'
        i += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (!inQuotes && char === delimiter) {
      currentRow.push(currentCell)
      currentCell = ''
      continue
    }
    if (!inQuotes && char === '\n') {
      currentRow.push(currentCell)
      rows.push(currentRow)
      currentRow = []
      currentCell = ''
      continue
    }
    currentCell += char
  }
  if (currentCell !== '' || currentRow.length) {
    currentRow.push(currentCell)
    rows.push(currentRow)
  }
  const normalizedRows = rows.map(row => row.map(cell => String(cell || '').trim())).filter(row => row.some(cell => cell !== ''))
  if (!normalizedRows.length) return { headers: [], rows: [] }
  const columnCount = Math.max(...normalizedRows.map(row => row.length))
  const normalizedMatrix = normalizedRows.map(row => Array.from({ length: columnCount }, (_, index) => row[index] || ''))
  return {
    headers: normalizedMatrix[0].map((cell, index) => cell || `列${index + 1}`),
    rows: normalizedMatrix.slice(1)
  }
}

export function collectXmlNodesByLocalName(root, localName) {
  const result = []
  const visit = (node) => {
    if (!node || !node.childNodes) return
    Array.from(node.childNodes).forEach((child) => {
      if (child.nodeType === 1) {
        if (child.localName === localName) result.push(child)
        visit(child)
      }
    })
  }
  visit(root.documentElement || root)
  return result
}

export function parseXmlText(xmlText = '') {
  if (!process.client || !window.DOMParser) return null
  try {
    return new window.DOMParser().parseFromString(xmlText, 'application/xml')
  } catch (error) {
    return null
  }
}

export function getColumnIndexByRef(ref = '') {
  const letters = String(ref || '').match(/[A-Za-z]+/)
  if (!letters) return 0
  return letters[0].toUpperCase().split('').reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0) - 1
}

export async function parseDocxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const documentEntry = zip.file('word/document.xml')
  if (!documentEntry) return { paragraphs: [] }
  const xmlText = await documentEntry.async('string')
  const doc = parseXmlText(xmlText)
  if (!doc) {
    const fallback = xmlText.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    return { paragraphs: fallback ? [fallback] : [] }
  }
  return {
    paragraphs: collectXmlNodesByLocalName(doc, 'p')
      .map(paragraph => collectXmlNodesByLocalName(paragraph, 't').map(node => node.textContent || '').join(''))
      .map(item => String(item || '').trim())
      .filter(Boolean)
  }
}

export async function parseXlsxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const sharedStringsEntry = zip.file('xl/sharedStrings.xml')
  let sharedStrings = []
  if (sharedStringsEntry) {
    const sharedDoc = parseXmlText(await sharedStringsEntry.async('string'))
    if (sharedDoc) {
      sharedStrings = collectXmlNodesByLocalName(sharedDoc, 'si').map(item => collectXmlNodesByLocalName(item, 't').map(node => node.textContent || '').join(''))
    }
  }
  const workbookEntry = zip.file('xl/workbook.xml')
  const workbookRelsEntry = zip.file('xl/_rels/workbook.xml.rels')
  if (!workbookEntry || !workbookRelsEntry) return { sheets: [] }
  const workbookDoc = parseXmlText(await workbookEntry.async('string'))
  const relsDoc = parseXmlText(await workbookRelsEntry.async('string'))
  if (!workbookDoc || !relsDoc) return { sheets: [] }
  const relMap = {}
  collectXmlNodesByLocalName(relsDoc, 'Relationship').forEach((relation) => {
    const id = relation.getAttribute('Id')
    const target = relation.getAttribute('Target')
    if (id && target) relMap[id] = target.replace(/^\/?/, '')
  })
  const sheets = []
  for (const sheetNode of collectXmlNodesByLocalName(workbookDoc, 'sheet').slice(0, 3)) {
    const name = sheetNode.getAttribute('name') || 'Sheet'
    const relationId = sheetNode.getAttribute('r:id') || sheetNode.getAttribute('id')
    const target = relMap[relationId]
    if (!target) continue
    const sheetEntry = zip.file(`xl/${target}`)
    if (!sheetEntry) continue
    const sheetDoc = parseXmlText(await sheetEntry.async('string'))
    if (!sheetDoc) continue
    const rows = []
    let maxColumnCount = 0
    collectXmlNodesByLocalName(sheetDoc, 'row').slice(0, 80).forEach((rowNode) => {
      const rowData = []
      collectXmlNodesByLocalName(rowNode, 'c').forEach((cellNode) => {
        const columnIndex = getColumnIndexByRef(cellNode.getAttribute('r') || '')
        const cellType = cellNode.getAttribute('t') || ''
        let value = ''
        if (cellType === 's') {
          const valueNode = collectXmlNodesByLocalName(cellNode, 'v')[0]
          const sharedIndex = Number(valueNode && valueNode.textContent)
          value = Number.isNaN(sharedIndex) ? '' : (sharedStrings[sharedIndex] || '')
        } else if (cellType === 'inlineStr') {
          value = collectXmlNodesByLocalName(cellNode, 't').map(node => node.textContent || '').join('')
        } else {
          const valueNode = collectXmlNodesByLocalName(cellNode, 'v')[0]
          value = valueNode ? valueNode.textContent || '' : ''
        }
        rowData[columnIndex] = String(value || '').trim()
      })
      maxColumnCount = Math.max(maxColumnCount, rowData.length)
      rows.push(rowData)
    })
    const normalizedRows = rows.map(row => Array.from({ length: maxColumnCount }, (_, index) => row[index] || '')).filter(row => row.some(cell => cell !== ''))
    if (normalizedRows.length) {
      sheets.push({
        name,
        headers: normalizedRows[0].map((cell, index) => cell || `列${index + 1}`),
        rows: normalizedRows.slice(1)
      })
    }
  }
  return { sheets }
}

export async function parsePptxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const slideEntries = Object.keys(zip.files)
    .filter(name => /^ppt\/slides\/slide\d+\.xml$/.test(name))
    .sort((a, b) => Number((a.match(/slide(\d+)\.xml/) || [])[1] || 0) - Number((b.match(/slide(\d+)\.xml/) || [])[1] || 0))
  const slides = []
  for (const [index, slideName] of slideEntries.slice(0, 20).entries()) {
    const entry = zip.file(slideName)
    if (!entry) continue
    const doc = parseXmlText(await entry.async('string'))
    if (!doc) continue
    const lines = collectXmlNodesByLocalName(doc, 't').map(node => String(node.textContent || '').trim()).filter(Boolean)
    slides.push({ index: index + 1, lines })
  }
  return { slides }
}

export function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {}
    return tags.split(',').map(v => v.trim()).filter(Boolean)
  }
  return []
}

export function toBackendDateTime(value) {
  if (!value) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function extractApiData(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
  if (
    payload &&
    typeof payload === 'object' &&
    payload.data !== undefined &&
    (payload.code !== undefined || payload.success !== undefined || payload.message !== undefined)
  ) {
    return payload.data
  }
  return payload
}

export function normalizeAiModel(item = {}) {
  const rawId = item.id ?? item.modelId ?? item.value ?? item.code ?? ''
  return {
    ...item,
    id: rawId === null || rawId === undefined ? '' : String(rawId),
    rawId,
    modelName: item.modelName || item.name || item.label || item.model || item.code || '',
    providerCode: item.providerCode || item.provider || item.providerName || item.vendor || '',
    isEnabled: item.isEnabled !== false
  }
}

export function buildProjectAiContent(project = {}, contributors = [], currentFile = {}) {
  const tags = parseTags(project.tags)
  const contributorNames = (contributors || [])
    .map(item => item && item.displayName)
    .filter(Boolean)

  const fileSummary = []
  if (Array.isArray(project.files) && project.files.length) {
    fileSummary.push(`文件数量：${project.files.length}`)
  }
  if (currentFile && currentFile.path) {
    fileSummary.push(`当前浏览文件：${currentFile.path}`)
  }

  return [
    `项目名称：${project.name || '未提供'}`,
    `项目描述：${project.description || '未提供'}`,
    `项目分类：${CATEGORY_MAP[project.category] || project.category || '未提供'}`,
    `项目状态：${STATUS_MAP[project.status] || project.status || '未提供'}`,
    `可见性：${project.visibility === 'private' ? '私有' : '公开'}`,
    `项目标签：${tags.length ? tags.join('、') : '未提供'}`,
    `作者：${project.authorName || '未提供'}`,
    `贡献者：${contributorNames.length ? contributorNames.join('、') : '未提供'}`,
    `项目文件：${fileSummary.length ? fileSummary.join('；') : '未提供'}`,
    `README：${project.readme || '未提供'}`
  ].join('\n')
}

export function decodeJwtPayload(token = '') {
  try {
    const parts = String(token || '').split('.')
    if (parts.length < 2) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + (4 - (base64.length % 4 || 4)) % 4, '=')
    const json = process.client ? window.atob(normalized) : Buffer.from(normalized, 'base64').toString('utf-8')
    return JSON.parse(json)
  } catch (e) {
    return null
  }
}

export function pickUserIdFromObject(source) {
  if (!source || typeof source !== 'object') return null
  const queue = [source]
  const seen = new Set()
  const keys = ['id', 'userId', 'uid', 'memberId', 'loginId', 'accountId', 'sub']

  while (queue.length) {
    const current = queue.shift()
    if (!current || typeof current !== 'object' || seen.has(current)) continue
    seen.add(current)

    for (const key of keys) {
      const value = current[key]
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        const text = String(value).trim()
        if (/^\d+$/.test(text)) return Number(text)
        return text
      }
    }

    ;['user', 'userInfo', 'profile', 'account', 'loginUser', 'currentUser', 'data'].forEach((key) => {
      if (current[key] && typeof current[key] === 'object') {
        queue.push(current[key])
      }
    })
  }

  return null
}

export function escapeHtmlValue(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function normalizeMarkdownUrl(url = '') {
  const value = String(url || '').trim()
  if (!value) return ''
  if (/^(https?:|mailto:|tel:)/i.test(value)) return value
  if (/^(\/|#|\.\/|\.\.\/)/.test(value)) return value
  return ''
}

export function renderMarkdownLink(label = '', url = '', title = '') {
  const safeUrl = normalizeMarkdownUrl(url)
  const safeLabel = escapeHtmlValue(label || url)
  const safeTitle = escapeHtmlValue(title || '')
  if (!safeUrl) return safeLabel
  const titleAttr = safeTitle ? ` title="${safeTitle}"` : ''
  return `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer nofollow"${titleAttr}>${safeLabel}</a>`
}

export function renderMarkdownImage(alt = '', url = '', title = '') {
  const safeUrl = normalizeMarkdownUrl(url)
  const safeAlt = escapeHtmlValue(alt || 'README 图片')
  const safeTitle = escapeHtmlValue(title || alt || 'README 图片')
  if (!safeUrl) return `<span class="markdown-image-alt">${safeAlt}</span>`
  return `<span class="markdown-image-wrap"><img src="${safeUrl}" alt="${safeAlt}" title="${safeTitle}" loading="lazy"></span>`
}

export function renderInlineMarkdown(text) {
  const tokens = []
  const pushToken = (html) => {
    const key = `@@MD_TOKEN_${tokens.length}@@`
    tokens.push(html)
    return key
  }

  let raw = String(text || '')
  raw = raw.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)/g, (_, alt, url, title) => pushToken(renderMarkdownImage(alt, url, title)))
  raw = raw.replace(/\[([^\]]+)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)/g, (_, label, url, title) => pushToken(renderMarkdownLink(label, url, title)))

  return escapeHtmlValue(raw)
    .replace(/&lt;br\s*\/?&gt;/gi, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/~~(.+?)~~/g, '<del>$1</del>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
    .replace(/@@MD_TOKEN_(\d+)@@/g, (_, index) => tokens[Number(index)] || '')
}

export function parseMarkdownTableCells(line) {
  return String(line || '')
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim())
}

export function isMarkdownTableSeparator(line) {
  const cells = parseMarkdownTableCells(line)
  return cells.length > 0 && cells.every(cell => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')))
}

export function looksLikeMarkdownTableRow(line) {
  const text = String(line || '').trim()
  return text.includes('|') && parseMarkdownTableCells(text).length >= 2
}

export function isSpecialMarkdownLine(line, nextLine) {
  const text = String(line || '').trim()
  if (!text) return true
  if (/^```/.test(text)) return true
  if (/^([-*_])\1{2,}$/.test(text)) return true
  if (/^#{1,6}\s+/.test(text)) return true
  if (/^>\s+/.test(text)) return true
  if (/^\s*[-*+]\s+/.test(text)) return true
  if (/^\s*\d+\.\s+/.test(text)) return true
  if (looksLikeMarkdownTableRow(text) && isMarkdownTableSeparator(nextLine)) return true
  return false
}

export function stripMarkdownToPlainText(source = '') {
  return String(source || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '$1 ')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '$1 ')
    .replace(/[`>#*_~|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function countReadmeReadableUnits(source = '') {
  const plain = stripMarkdownToPlainText(source)
  if (!plain) return 0
  const chineseCount = (plain.match(/[\u4e00-\u9fff]/g) || []).length
  const latinCount = (plain.match(/[A-Za-z0-9_]+/g) || []).length
  return chineseCount + latinCount
}

export function countMarkdownHeadings(source = '') {
  return String(source || '')
    .split('\n')
    .filter(line => /^\s*#{1,6}\s+/.test(String(line || '')))
    .length
}

export function countMarkdownCodeBlocks(source = '') {
  const matches = String(source || '').match(/```/g)
  return matches ? Math.floor(matches.length / 2) : 0
}

export function buildReadmeLeadText(source = '', fallback = '') {
  const plain = stripMarkdownToPlainText(source)
  if (plain) return plain.slice(0, 96)
  return String(fallback || '当前项目还没有补充 README 内容').trim()
}

export function formatEstimatedReadTime(source = '') {
  const units = countReadmeReadableUnits(source)
  if (!units) return '少于 1 分钟'
  const minutes = Math.max(1, Math.ceil(units / 360))
  return `${minutes} 分钟`
}

export function renderMarkdownToHtml(source, emptyText = '暂无内容') {
  const raw = String(source || '').replace(/\r\n?/g, '\n').trim()
  if (!raw) {
    return `<div class="empty-readme">${escapeHtmlValue(emptyText)}</div>`
  }

  const lines = raw.split('\n')
  const blocks = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = String(line || '').trim()

    if (!trimmed) {
      i += 1
      continue
    }

    if (/^```/.test(trimmed)) {
      const fenceMatch = trimmed.match(/^```\s*([A-Za-z0-9_+-]*)\s*$/)
      const codeLang = fenceMatch && fenceMatch[1] ? fenceMatch[1].toLowerCase() : ''
      const codeLines = []
      i += 1
      while (i < lines.length && !/^```/.test(String(lines[i] || '').trim())) {
        codeLines.push(lines[i])
        i += 1
      }
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) {
        i += 1
      }
      const codeText = codeLines.join('\n')
      const language = getHighlightLanguage(codeLang)
      let codeHtml = escapeHtmlValue(codeText)
      try {
        codeHtml = hljs.highlight(codeText, { language, ignoreIllegals: true }).value
      } catch (error) {}
      const langLabel = escapeHtmlValue(codeLang || 'text').toUpperCase()
      blocks.push(`<div class="markdown-code-block"><div class="markdown-code-head"><span class="markdown-code-lang">${langLabel}</span></div><pre><code class="hljs language-${escapeHtmlValue(language)}">${codeHtml}</code></pre></div>`)
      continue
    }

    if (/^([-*_])\1{2,}$/.test(trimmed)) {
      blocks.push('<hr>')
      i += 1
      continue
    }

    if (/^#{1,6}\s+/.test(trimmed)) {
      const level = trimmed.match(/^#+/)[0].length
      const content = trimmed.slice(level).trim()
      blocks.push(`<h${level}>${renderInlineMarkdown(content)}</h${level}>`)
      i += 1
      continue
    }

    if (looksLikeMarkdownTableRow(trimmed) && isMarkdownTableSeparator(lines[i + 1])) {
      const headers = parseMarkdownTableCells(trimmed)
      i += 2
      const rows = []
      while (i < lines.length) {
        const rowLine = String(lines[i] || '').trim()
        if (!rowLine || !looksLikeMarkdownTableRow(rowLine)) break
        if (isMarkdownTableSeparator(rowLine)) {
          i += 1
          continue
        }
        rows.push(parseMarkdownTableCells(rowLine))
        i += 1
      }
      const thead = `<thead><tr>${headers.map(cell => `<th>${renderInlineMarkdown(cell)}</th>`).join('')}</tr></thead>`
      const tbody = rows.length
        ? `<tbody>${rows.map(row => `<tr>${headers.map((_, idx) => `<td>${renderInlineMarkdown(row[idx] || '')}</td>`).join('')}</tr>`).join('')}</tbody>`
        : ''
      blocks.push(`<div class="markdown-table-wrap"><table>${thead}${tbody}</table></div>`)
      continue
    }

    if (/^!\[[^\]]*\]\([^)]+\)$/.test(trimmed)) {
      blocks.push(`<p class="markdown-image-only">${renderInlineMarkdown(trimmed)}</p>`)
      i += 1
      continue
    }

    if (/^>\s+/.test(trimmed)) {
      const quoteLines = []
      while (i < lines.length && /^>\s+/.test(String(lines[i] || '').trim())) {
        quoteLines.push(String(lines[i] || '').trim().replace(/^>\s+/, ''))
        i += 1
      }
      blocks.push(`<blockquote>${quoteLines.map(item => renderInlineMarkdown(item)).join('<br>')}</blockquote>`)
      continue
    }

    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
        const rawItem = String(lines[i] || '').replace(/^\s*[-*+]\s+/, '')
        const taskMatch = rawItem.match(/^\[( |x|X)\]\s+(.*)$/)
        if (taskMatch) {
          const checked = String(taskMatch[1] || '').toLowerCase() === 'x'
          items.push(`<li class="markdown-task-item${checked ? ' is-checked' : ''}"><span class="markdown-task-box">${checked ? '√' : ''}</span><span>${renderInlineMarkdown(taskMatch[2] || '')}</span></li>`)
        } else {
          items.push(`<li>${renderInlineMarkdown(rawItem)}</li>`)
        }
        i += 1
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
      continue
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(renderInlineMarkdown(String(lines[i] || '').replace(/^\s*\d+\.\s+/, '')))
        i += 1
      }
      blocks.push(`<ol>${items.map(item => `<li>${item}</li>`).join('')}</ol>`)
      continue
    }

    const paragraphLines = []
    while (i < lines.length) {
      const current = String(lines[i] || '')
      const currentTrimmed = current.trim()
      if (!currentTrimmed) {
        i += 1
        break
      }
      if (paragraphLines.length > 0 && isSpecialMarkdownLine(current, lines[i + 1])) {
        break
      }
      paragraphLines.push(renderInlineMarkdown(currentTrimmed))
      i += 1
    }

    if (paragraphLines.length) {
      blocks.push(`<p>${paragraphLines.join('<br>')}</p>`)
    }
  }

  return blocks.join('')
}
