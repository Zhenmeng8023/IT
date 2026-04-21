const DEFAULT_LIMITS = Object.freeze({
  readmeMaxChars: 2200,
  readmeSummaryChars: 900,
  docCount: 3,
  docSummaryChars: 800,
  keyFileCount: 4,
  keyFileSummaryChars: 700,
  mergedDescriptionChars: 1200
})

function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function clipText(value, maxChars = 1000) {
  const text = normalizeText(value)
  if (!text) return ''
  if (text.length <= maxChars) return text
  return `${text.slice(0, Math.max(0, maxChars - 3))}...`
}

function normalizePath(value) {
  return String(value || '')
    .replace(/\\/g, '/')
    .replace(/^[A-Za-z]:\/+/, '')
    .replace(/^\/+/, '')
    .replace(/\/+/g, '/')
    .replace(/\/$/, '')
    .trim()
}

function resolveFilePath(file = {}) {
  const relativePath = normalizePath(file.relativePath || file.relative_file_path || '')
  if (relativePath) return relativePath
  const fileName = normalizePath(file.fileName || file.file_name || file.name || '')
  if (fileName) return fileName
  return normalizePath(file.path || file.filePath || file.file_path || '')
}

function summarizeTextBlock(value, maxChars = 800) {
  const text = String(value || '').replace(/\r\n/g, '\n').trim()
  if (!text) return ''
  const lines = text
    .split('\n')
    .map(line => line.trimEnd())
    .filter((line, index) => line.trim() || index < 2)
  if (!lines.length) return ''

  const picked = []
  let totalChars = 0
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const nextLength = totalChars + line.length + 1
    if (nextLength > maxChars) break
    picked.push(line)
    totalChars = nextLength
    if (picked.length >= 24) break
  }
  return clipText(picked.join('\n').trim(), maxChars)
}

function buildProjectBasicInfo(project = {}) {
  const tags = Array.isArray(project.tags)
    ? project.tags.map(item => normalizeText(item && (item.name || item.label || item.value || item))).filter(Boolean)
    : String(project.tags || '')
      .split(',')
      .map(item => normalizeText(item))
      .filter(Boolean)

  return {
    id: project.id || null,
    name: normalizeText(project.name || project.projectName || project.title),
    description: normalizeText(project.description || project.summary || ''),
    category: normalizeText(project.category),
    status: normalizeText(project.status || project.statusName),
    visibility: normalizeText(project.visibility),
    tags: tags.slice(0, 12),
    stars: Number(project.stars || 0),
    views: Number(project.views || 0),
    downloads: Number(project.downloads || 0),
    updatedAt: normalizeText(project.updatedAt)
  }
}

function scoreCandidateByPath(pathLower = '') {
  if (!pathLower) return { score: 0, reason: '' }
  if (/^readme\.md$/.test(pathLower) || /(^|\/)readme\.md$/.test(pathLower)) {
    return { score: 120, reason: 'readme' }
  }
  if (/^docs\/.+\.md$/.test(pathLower) || /(^|\/)docs\/.+\.md$/.test(pathLower)) {
    return { score: 110, reason: 'docs-markdown' }
  }
  if (/(^|\/)package\.json$/.test(pathLower)) {
    return { score: 105, reason: 'package-json' }
  }
  if (/(^|\/)pom\.xml$/.test(pathLower)) {
    return { score: 100, reason: 'pom-xml' }
  }
  if (/(^|\/)application\.java$/.test(pathLower)) {
    return { score: 98, reason: 'application-java' }
  }
  if (/(^|\/)vite\.config(\.[a-z0-9_-]+)?\.(js|ts|mjs|cjs)$/.test(pathLower)) {
    return { score: 96, reason: 'vite-config' }
  }
  if (/(^|\/)nuxt\.config(\.[a-z0-9_-]+)?\.(js|ts|mjs|cjs)$/.test(pathLower)) {
    return { score: 96, reason: 'nuxt-config' }
  }
  return { score: 0, reason: '' }
}

function pickKeyFileCandidates(projectFiles = [], maxCount = 4) {
  const list = Array.isArray(projectFiles) ? projectFiles : []
  const candidates = list
    .map(file => {
      const path = resolveFilePath(file)
      const pathLower = path.toLowerCase()
      const match = scoreCandidateByPath(pathLower)
      if (!match.score) return null
      return {
        file,
        id: file.id || null,
        path,
        reason: match.reason,
        score: match.score,
        size: Number(file.fileSizeBytes || file.file_size_bytes || file.size || 0)
      }
    })
    .filter(Boolean)
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score
      return a.path.localeCompare(b.path, 'zh-CN')
    })

  const dedupMap = new Map()
  candidates.forEach(item => {
    if (!item.path) return
    if (!dedupMap.has(item.path.toLowerCase())) {
      dedupMap.set(item.path.toLowerCase(), item)
    }
  })

  return Array.from(dedupMap.values()).slice(0, maxCount)
}

async function buildDocsContext(projectDocs = [], options = {}) {
  const docs = Array.isArray(projectDocs) ? projectDocs.slice(0, options.docCount) : []
  const items = []

  for (let i = 0; i < docs.length; i += 1) {
    const doc = docs[i] || {}
    const rawContent = normalizeText(doc.content)
    let content = rawContent
    if (!content && typeof options.loadDocContent === 'function') {
      try {
        content = normalizeText(await options.loadDocContent(doc))
      } catch (error) {
        content = ''
      }
    }
    items.push({
      id: doc.id || null,
      title: normalizeText(doc.title) || `doc-${i + 1}`,
      updatedAt: normalizeText(doc.updatedAt || doc.createdAt),
      summary: summarizeTextBlock(content, options.docSummaryChars),
      hasContent: !!content
    })
  }

  const availableCount = items.filter(item => item.hasContent).length
  return {
    total: Array.isArray(projectDocs) ? projectDocs.length : 0,
    sampled: items.length,
    availableCount,
    items
  }
}

async function buildKeyFileContext(projectFiles = [], options = {}) {
  const candidates = pickKeyFileCandidates(projectFiles, options.keyFileCount)
  const items = []
  for (let i = 0; i < candidates.length; i += 1) {
    const candidate = candidates[i]
    let content = ''
    if (typeof options.loadFileContent === 'function') {
      try {
        content = normalizeText(await options.loadFileContent(candidate.file))
      } catch (error) {
        content = ''
      }
    }
    items.push({
      id: candidate.id,
      path: candidate.path,
      reason: candidate.reason,
      size: candidate.size,
      summary: summarizeTextBlock(content, options.keyFileSummaryChars),
      hasContent: !!content
    })
  }

  return {
    sampled: items.length,
    availableCount: items.filter(item => item.hasContent).length,
    items
  }
}

function buildCoverageSummary({ hasBasics, hasReadme, docCoverage, keyFileCoverage }) {
  const basicWeight = hasBasics ? 30 : 0
  const readmeWeight = hasReadme ? 25 : 0
  const docsWeight = Math.round(20 * docCoverage)
  const keyFilesWeight = Math.round(25 * keyFileCoverage)
  const score = Math.max(0, Math.min(100, basicWeight + readmeWeight + docsWeight + keyFilesWeight))

  const missing = []
  if (!hasBasics) missing.push('project-basic')
  if (!hasReadme) missing.push('readme')
  if (docCoverage <= 0) missing.push('docs')
  if (keyFileCoverage <= 0) missing.push('key-files')

  let hint = 'low'
  if (score >= 75) hint = 'high'
  else if (score >= 45) hint = 'medium'

  return {
    score,
    hint,
    insufficient: {
      hasGap: missing.length > 0,
      missing
    }
  }
}

export async function collectProjectAiContextEnhancement(payload = {}) {
  const project = payload.project || {}
  const limits = { ...DEFAULT_LIMITS, ...(payload.limits || {}) }
  const projectBasicInfo = buildProjectBasicInfo(project)
  const hasBasics = !!(projectBasicInfo.name || projectBasicInfo.description)

  const readmeRaw = clipText(normalizeText(project.readme), limits.readmeMaxChars)
  const readmeSummary = summarizeTextBlock(readmeRaw, limits.readmeSummaryChars)
  const hasReadme = !!readmeRaw

  const docsContext = await buildDocsContext(payload.projectDocs || [], {
    docCount: limits.docCount,
    docSummaryChars: limits.docSummaryChars,
    loadDocContent: payload.loadDocContent
  })
  const keyFileContext = await buildKeyFileContext(payload.projectFiles || [], {
    keyFileCount: limits.keyFileCount,
    keyFileSummaryChars: limits.keyFileSummaryChars,
    loadFileContent: payload.loadFileContent
  })

  const docCoverage = docsContext.sampled > 0 ? docsContext.availableCount / docsContext.sampled : 0
  const keyFileCoverage = keyFileContext.sampled > 0 ? keyFileContext.availableCount / keyFileContext.sampled : 0
  const coverage = buildCoverageSummary({
    hasBasics,
    hasReadme,
    docCoverage,
    keyFileCoverage
  })

  const sources = [
    {
      source: 'project.basic',
      status: hasBasics ? 'ready' : 'missing'
    },
    {
      source: 'project.readme',
      status: hasReadme ? 'ready' : 'missing',
      title: normalizeText(project.readmeTitle || 'README')
    },
    {
      source: 'project.docs',
      status: docsContext.availableCount > 0 ? 'ready' : (docsContext.sampled > 0 ? 'partial' : 'missing'),
      sampled: docsContext.sampled,
      available: docsContext.availableCount
    },
    {
      source: 'project.branch.key-files',
      status: keyFileContext.availableCount > 0 ? 'ready' : (keyFileContext.sampled > 0 ? 'partial' : 'missing'),
      sampled: keyFileContext.sampled,
      available: keyFileContext.availableCount
    }
  ]

  return {
    generatedAt: new Date().toISOString(),
    branch: payload.currentBranch
      ? {
        id: payload.currentBranch.id || null,
        name: normalizeText(payload.currentBranch.name),
        isDefault: !!payload.currentBranch.isDefault
      }
      : null,
    sources,
    coverageScore: coverage.score,
    coverageHint: coverage.hint,
    contextInsufficient: coverage.insufficient,
    projectBasicInfo,
    readmeContext: {
      title: normalizeText(project.readmeTitle || 'README'),
      source: normalizeText(project.readmeSource || ''),
      content: readmeRaw,
      summary: readmeSummary,
      hasContent: hasReadme
    },
    docsContext,
    keyFileContext
  }
}

function buildMergedProjectDescription(basePayload = {}, enhanced = {}, maxChars = 1200) {
  const pieces = []
  const baseDescription = normalizeText(basePayload.projectDescription)
  if (baseDescription) pieces.push(baseDescription)

  const readmeSummary = normalizeText(enhanced && enhanced.readmeContext && enhanced.readmeContext.summary)
  if (readmeSummary) {
    pieces.push(`README summary:\n${readmeSummary}`)
  }

  const docLines = (enhanced && enhanced.docsContext && Array.isArray(enhanced.docsContext.items))
    ? enhanced.docsContext.items
      .filter(item => normalizeText(item.summary))
      .slice(0, 2)
      .map(item => `${normalizeText(item.title)}: ${normalizeText(item.summary)}`)
    : []
  if (docLines.length) {
    pieces.push(`Docs:\n${docLines.join('\n')}`)
  }

  const merged = pieces.filter(Boolean).join('\n\n')
  return clipText(merged, maxChars)
}

export function mergeProjectDetailContextPayload(basePayload = {}, enhancedPayload = null) {
  const base = basePayload && typeof basePayload === 'object' ? basePayload : {}
  const enhanced = enhancedPayload && typeof enhancedPayload === 'object' ? enhancedPayload : null
  if (!enhanced) return { ...base }

  const mergedDescription = buildMergedProjectDescription(base, enhanced, DEFAULT_LIMITS.mergedDescriptionChars)
  const mergedReadmeLead = clipText(
    normalizeText(
      (enhanced.readmeContext && enhanced.readmeContext.summary)
        || base.readmeLeadText
    ),
    1200
  )

  return {
    ...base,
    projectDescription: mergedDescription || base.projectDescription,
    readmeLeadText: mergedReadmeLead || base.readmeLeadText,
    sources: Array.isArray(enhanced.sources) ? enhanced.sources : [],
    coverageScore: Number(enhanced.coverageScore || 0),
    coverageHint: normalizeText(enhanced.coverageHint) || 'low',
    contextInsufficient: enhanced.contextInsufficient || { hasGap: true, missing: ['enhanced-context-unavailable'] },
    enhancedContext: {
      branch: enhanced.branch || null,
      generatedAt: enhanced.generatedAt || '',
      projectBasicInfo: enhanced.projectBasicInfo || {},
      readmeContext: enhanced.readmeContext || {},
      docsContext: enhanced.docsContext || { total: 0, sampled: 0, availableCount: 0, items: [] },
      keyFileContext: enhanced.keyFileContext || { sampled: 0, availableCount: 0, items: [] }
    }
  }
}

