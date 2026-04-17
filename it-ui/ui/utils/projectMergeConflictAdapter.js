const CONFLICT_TYPE_META = {
  CONTENT_CONFLICT: {
    label: '内容冲突',
    tone: 'danger',
    icon: 'el-icon-warning-outline',
    description: '源分支和目标分支在同一文件内容上发生了修改冲突'
  },
  DELETE_MODIFY_CONFLICT: {
    label: '删除/修改冲突',
    tone: 'warning',
    icon: 'el-icon-delete',
    description: '一侧删除，另一侧修改，通常需要先确认保留哪一侧的结果'
  },
  RENAME_CONFLICT: {
    label: '重命名冲突',
    tone: 'info',
    icon: 'el-icon-refresh',
    description: '文件重命名后与目标分支的路径或内容发生冲突'
  },
  MOVE_CONFLICT: {
    label: '移动冲突',
    tone: 'success',
    icon: 'el-icon-position',
    description: '文件移动后与目标分支的路径或内容发生冲突'
  },
  TARGET_PATH_OCCUPIED: {
    label: '目标路径被占用',
    tone: 'warning',
    icon: 'el-icon-folder-opened',
    description: '目标路径已经存在同名文件或目录'
  },
  STALE_BRANCH: {
    label: '分支落后',
    tone: 'warning',
    icon: 'el-icon-alarm-clock',
    description: '这不是普通内容冲突，需要先更新源分支并重新检查'
  }
}

const CONFLICT_TYPE_ALIASES = {
  CONTENT: 'CONTENT_CONFLICT',
  CONTENT_CONFLICT: 'CONTENT_CONFLICT',
  DELETE_MODIFY: 'DELETE_MODIFY_CONFLICT',
  DELETE_MODIFY_CONFLICT: 'DELETE_MODIFY_CONFLICT',
  RENAME: 'RENAME_CONFLICT',
  RENAME_CONFLICT: 'RENAME_CONFLICT',
  MOVE: 'MOVE_CONFLICT',
  MOVE_CONFLICT: 'MOVE_CONFLICT',
  TARGET_PATH_OCCUPIED: 'TARGET_PATH_OCCUPIED',
  STALE_BRANCH: 'STALE_BRANCH'
}

const CONFLICT_ORDER = {
  CONTENT_CONFLICT: 0,
  DELETE_MODIFY_CONFLICT: 1,
  RENAME_CONFLICT: 2,
  MOVE_CONFLICT: 3,
  TARGET_PATH_OCCUPIED: 4,
  STALE_BRANCH: 5
}

const PATH_CONFLICT_ACTIONS = {
  RENAME_CONFLICT: {
    source: {
      label: '保留源分支版本',
      resolutionStrategy: 'USE_SOURCE_PATH'
    },
    target: {
      label: '保留目标分支版本',
      resolutionStrategy: 'USE_TARGET_PATH'
    }
  },
  MOVE_CONFLICT: {
    source: {
      label: '保留源分支版本',
      resolutionStrategy: 'USE_SOURCE_PATH'
    },
    target: {
      label: '保留目标分支版本',
      resolutionStrategy: 'USE_TARGET_PATH'
    }
  },
  TARGET_PATH_OCCUPIED: {
    source: {
      label: '保留源分支版本',
      resolutionStrategy: 'KEEP_SOURCE'
    },
    target: {
      label: '保留目标分支版本',
      resolutionStrategy: 'KEEP_TARGET'
    }
  },
  DELETE_MODIFY_CONFLICT: {
    source: {
      label: '保留源分支版本',
      resolutionStrategy: 'KEEP_SOURCE'
    },
    target: {
      label: '保留目标分支版本',
      resolutionStrategy: 'KEEP_TARGET'
    }
  }
}

export const CONTENT_BLOCK_CHOICES = {
  KEEP_SOURCE: 'keepSource',
  KEEP_TARGET: 'keepTarget',
  KEEP_BOTH_SOURCE_THEN_TARGET: 'keepBothSourceThenTarget',
  KEEP_BOTH_TARGET_THEN_SOURCE: 'keepBothTargetThenSource',
  MANUAL: 'manual'
}

const CONTENT_BLOCK_CHOICE_ALIAS_MAP = {
  keepsource: CONTENT_BLOCK_CHOICES.KEEP_SOURCE,
  keeptarget: CONTENT_BLOCK_CHOICES.KEEP_TARGET,
  keepbothsourcethentarget: CONTENT_BLOCK_CHOICES.KEEP_BOTH_SOURCE_THEN_TARGET,
  keepbothtargetthensource: CONTENT_BLOCK_CHOICES.KEEP_BOTH_TARGET_THEN_SOURCE,
  manual: CONTENT_BLOCK_CHOICES.MANUAL
}

function asArray(value) {
  return Array.isArray(value) ? value : []
}

export function splitTextLines(value) {
  return String(value == null ? '' : value).replace(/\r\n/g, '\n').split('\n')
}

export function joinTextLines(lines) {
  return asArray(lines).map(item => (item == null ? '' : String(item))).join('\n')
}

export function normalizeContentBlockChoice(value, fallback = CONTENT_BLOCK_CHOICES.KEEP_SOURCE) {
  const raw = String(value || '')
    .trim()
    .replace(/[_-]+/g, '')
    .toLowerCase()
  return CONTENT_BLOCK_CHOICE_ALIAS_MAP[raw] || fallback
}

export function getContentBlockChoiceLabel(choice, branchNames = {}) {
  const sourceBranchName = String(branchNames.sourceBranchName || '源分支')
  const targetBranchName = String(branchNames.targetBranchName || '目标分支')
  const normalized = normalizeContentBlockChoice(choice)
  switch (normalized) {
    case CONTENT_BLOCK_CHOICES.KEEP_SOURCE:
      return `保留源分支（${sourceBranchName}）`
    case CONTENT_BLOCK_CHOICES.KEEP_TARGET:
      return `保留目标分支（${targetBranchName}）`
    case CONTENT_BLOCK_CHOICES.KEEP_BOTH_SOURCE_THEN_TARGET:
      return `两边都保留（${sourceBranchName} 在前）`
    case CONTENT_BLOCK_CHOICES.KEEP_BOTH_TARGET_THEN_SOURCE:
      return `两边都保留（${targetBranchName} 在前）`
    case CONTENT_BLOCK_CHOICES.MANUAL:
      return '手动编辑'
    default:
      return normalized
  }
}

function uniqueStrings(values) {
  return Array.from(
    new Set(
      asArray(values)
        .map(item => String(item || '').trim())
        .filter(Boolean)
    )
  )
}

function normalizeEffectiveCheck(raw) {
  const item = raw && typeof raw === 'object' ? raw : {}
  return {
    id: item.id ?? null,
    checkType: String(item.checkType || 'custom'),
    checkStatus: String(item.checkStatus || ''),
    commitId: item.commitId ?? null,
    mergeRequestId: item.mergeRequestId ?? null,
    summary: String(item.summary || ''),
    createdAt: item.createdAt || '',
    blockingMerge: !!item.blockingMerge,
    systemInternal: !!item.systemInternal
  }
}

function normalizeEffectiveChecks(values) {
  return asArray(values).map(normalizeEffectiveCheck)
}

function getConflictPathFields(conflict) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  return {
    basePath: String(item.basePath || item.oldPath || ''),
    sourcePath: String(item.sourcePath || item.newPath || ''),
    targetPath: String(item.targetPath || item.path || ''),
    filePath: String(item.filePath || item.path || item.newPath || item.sourcePath || item.targetPath || item.oldPath || '')
  }
}

export function unwrapApiResponse(response) {
  if (response && typeof response === 'object' && Object.prototype.hasOwnProperty.call(response, 'data')) {
    const inner = response.data
    if (inner && typeof inner === 'object' && Object.prototype.hasOwnProperty.call(inner, 'code')) {
      return inner.data
    }
    return inner
  }
  if (response && typeof response === 'object' && Object.prototype.hasOwnProperty.call(response, 'code')) {
    return response.data
  }
  return response
}

export function normalizeConflictType(value) {
  const raw = String(value || '')
    .trim()
    .replace(/[\s-]+/g, '_')
    .toUpperCase()
  return CONFLICT_TYPE_ALIASES[raw] || raw
}

export function getConflictTypeMeta(value) {
  const type = normalizeConflictType(value)
  return CONFLICT_TYPE_META[type] || {
    label: type || '未知',
    tone: 'info',
    icon: 'el-icon-document',
    description: '未识别的冲突类型'
  }
}

export function getConflictPathCandidates(conflict) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  return uniqueStrings([
    item.basePath,
    item.sourcePath,
    item.targetPath,
    item.filePath,
    item.oldPath,
    item.newPath,
    item.path
  ])
}

export function hasPathConflictInfo(conflict) {
  return getConflictPathCandidates(conflict).length > 0
}

export function isPathConflictType(value, conflict) {
  const type = normalizeConflictType(value || (conflict && (conflict.conflictType || conflict.type)))
  if (!type) return false
  if (type === 'STALE_BRANCH') return false
  if (type === 'DELETE_MODIFY_CONFLICT') {
    return hasPathConflictInfo(conflict)
  }
  return ['RENAME_CONFLICT', 'MOVE_CONFLICT', 'TARGET_PATH_OCCUPIED'].includes(type)
}

export function getPathConflictActionMeta(conflict) {
  const type = normalizeConflictType(conflict && (conflict.conflictType || conflict.type))
  const actions = PATH_CONFLICT_ACTIONS[type]
  if (!actions) return []
  return ['source', 'target']
    .map(choice => {
      const action = actions[choice]
      if (!action) return null
      return {
        choice,
        label: action.label,
        resolutionStrategy: action.resolutionStrategy,
        tone: choice === 'source' ? 'primary' : 'success'
      }
    })
    .filter(Boolean)
}

export function buildPathConflictResolutionOption(conflict, choice) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  const type = normalizeConflictType(item.conflictType || item.type)
  const action = PATH_CONFLICT_ACTIONS[type] && PATH_CONFLICT_ACTIONS[type][choice]
  const conflictId = item.conflictId ?? item.id ?? item.filePath ?? item.path ?? item.oldPath ?? item.newPath
  if (!action || !conflictId) return null
  const option = {
    conflictId: String(conflictId),
    conflictType: type,
    resolutionStrategy: action.resolutionStrategy
  }
  return option
}

export function getConflictDisplayPath(conflict) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  const type = normalizeConflictType(item.conflictType || item.type)
  const { basePath, sourcePath, targetPath, filePath } = getConflictPathFields(item)
  const primary = filePath || targetPath || sourcePath || basePath || ''

  if ((type === 'RENAME_CONFLICT' || type === 'MOVE_CONFLICT') && (sourcePath || basePath) && targetPath) {
    const left = basePath || sourcePath || primary
    const right = targetPath || primary
    if (left && right && left !== right) {
      return `${left} -> ${right}`
    }
  }

  if (type === 'TARGET_PATH_OCCUPIED' && targetPath) {
    return targetPath
  }

  if (type === 'DELETE_MODIFY_CONFLICT') {
    return [basePath, sourcePath, targetPath].filter(Boolean).join(' -> ') || primary
  }

  return primary || '-'
}

export function getConflictDisplaySummary(conflict) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  if (item.summary) return item.summary
  if (item.suggestedAction) return item.suggestedAction
  const meta = getConflictTypeMeta(item.conflictType || item.type)
  return meta.description
}

export function normalizeMergeConflict(conflict = {}, index = 0) {
  const raw = conflict && typeof conflict === 'object' ? conflict : {}
  const type = normalizeConflictType(raw.conflictType || raw.type || raw.kind)
  const meta = getConflictTypeMeta(type)
  const conflictId = raw.conflictId ?? raw.id ?? raw.fileId ?? raw.filePath ?? raw.path ?? raw.oldPath ?? raw.newPath ?? `${type}:${index}`
  const pathFields = getConflictPathFields(raw)
  const pathCandidates = getConflictPathCandidates(raw)

  return {
    ...raw,
    conflictId: String(conflictId || `${type}:${index}`),
    conflictType: type,
    type,
    typeLabel: meta.label,
    typeTone: meta.tone,
    typeIcon: meta.icon,
    typeDescription: meta.description,
    filePath: raw.filePath || raw.path || '',
    basePath: pathFields.basePath,
    sourcePath: pathFields.sourcePath,
    targetPath: pathFields.targetPath,
    pathCandidates,
    hasPathInfo: pathCandidates.length > 0,
    displayPath: getConflictDisplayPath(raw),
    displaySummary: getConflictDisplaySummary(raw),
    severity: String(raw.severity || raw.level || '').toLowerCase()
  }
}

export function sortMergeConflicts(conflicts = []) {
  return asArray(conflicts)
    .map((item, index) => normalizeMergeConflict(item, index))
    .sort((left, right) => {
      const leftOrder = CONFLICT_ORDER[left.conflictType] ?? 99
      const rightOrder = CONFLICT_ORDER[right.conflictType] ?? 99
      if (leftOrder !== rightOrder) return leftOrder - rightOrder
      return String(left.displayPath || left.conflictId).localeCompare(String(right.displayPath || right.conflictId))
    })
}

export function normalizeMergeCheckDetail(response, context = {}) {
  const raw = unwrapApiResponse(response) || {}
  const source = raw && typeof raw === 'object' ? raw : {}
  const nested = source.latestMergeCheck || source.mergeCheck || source.preMergeCheck || source.detail || source.data || source
  const payload = nested && typeof nested === 'object' ? nested : {}
  const conflicts = sortMergeConflicts(
    payload.conflicts ||
    payload.items ||
    payload.conflictList ||
    source.conflicts ||
    source.items ||
    []
  )
  const blockingReasons = asArray(payload.blockingReasons || payload.reasons).filter(Boolean).map(item => String(item))
  const conflictCountRaw = Number(payload.conflictCount ?? payload.totalConflicts ?? source.conflictCount ?? source.totalConflicts)
  const conflictCount = Number.isFinite(conflictCountRaw) && conflictCountRaw >= 0 ? conflictCountRaw : conflicts.length
  const mergeable = typeof payload.mergeable === 'boolean'
    ? payload.mergeable
    : typeof source.mergeable === 'boolean'
      ? source.mergeable
      : conflicts.length === 0
  const summary = payload.summary || source.summary || ''
  const suggestedAction = payload.suggestedAction || source.suggestedAction || ''
  const checkedAt = payload.checkedAt || payload.createdAt || source.checkedAt || source.createdAt || ''
  const effectiveChecks = normalizeEffectiveChecks(payload.effectiveChecks || source.effectiveChecks || [])
  const blockingChecks = normalizeEffectiveChecks(
    payload.blockingChecks ||
    source.blockingChecks ||
    effectiveChecks.filter(item => item.checkStatus === 'failed' && item.blockingMerge)
  )

  return {
    ...payload,
    projectId: context.projectId ?? payload.projectId ?? source.projectId ?? null,
    mergeRequestId: context.mergeRequestId ?? payload.mergeRequestId ?? source.mergeRequestId ?? null,
    title: payload.title || source.title || context.title || '',
    sourceBranchId: payload.sourceBranchId ?? source.sourceBranchId ?? '',
    sourceBranchName: payload.sourceBranchName || source.sourceBranchName || '',
    targetBranchId: payload.targetBranchId ?? source.targetBranchId ?? '',
    targetBranchName: payload.targetBranchName || source.targetBranchName || '',
    sourceCommitId: payload.sourceCommitId || payload.sourceHeadCommitId || source.sourceCommitId || source.sourceHeadCommitId || '',
    targetCommitId: payload.targetCommitId || payload.targetHeadCommitId || source.targetCommitId || source.targetHeadCommitId || '',
    baseCommitId: payload.baseCommitId || source.baseCommitId || '',
    mergeable,
    hasConflict: typeof payload.hasConflict === 'boolean' ? payload.hasConflict : conflicts.length > 0,
    conflictCount,
    summary,
    suggestedAction,
    blockingReasons,
    requiresRecheck: !!(payload.requiresRecheck || source.requiresRecheck),
    requiresBranchUpdate: !!(payload.requiresBranchUpdate || source.requiresBranchUpdate),
    checkedAt,
    effectiveChecks,
    blockingChecks,
    conflicts,
    raw: source
  }
}

export function buildMergeCheckSummaryCards(detail = {}) {
  const mergeable = !!detail.mergeable
  const conflictCount = Number(detail.conflictCount || (detail.conflicts && detail.conflicts.length) || 0)
  const sourceLabel = detail.sourceBranchName || detail.sourceBranchId || '-'
  const targetLabel = detail.targetBranchName || detail.targetBranchId || '-'

  return [
    {
      key: 'conflicts',
      label: '冲突数量',
      value: conflictCount,
      desc: conflictCount > 0 ? '需要逐项处理的冲突条目' : '当前没有结构化冲突',
      tone: conflictCount > 0 ? 'danger' : 'success'
    },
    {
      key: 'mergeable',
      label: '预合并结论',
      value: mergeable ? '可合并' : '受阻',
      desc: detail.suggestedAction || detail.summary || (mergeable ? '最新检查通过' : '请先处理阻塞项'),
      tone: mergeable ? 'success' : 'warning'
    },
    {
      key: 'source',
      label: '源分支',
      value: sourceLabel,
      desc: detail.sourceCommitId || detail.sourceHeadCommitId || '-',
      tone: 'blue'
    },
    {
      key: 'target',
      label: '目标分支',
      value: targetLabel,
      desc: detail.targetCommitId || detail.targetHeadCommitId || '-',
      tone: 'cyan'
    }
  ]
}

export function getMergeCheckStatusLabel(detail = {}) {
  if (detail.mergeable === true) return '可合并'
  if (detail.mergeable === false) return '存在阻塞'
  if (detail.conflictCount || (detail.conflicts && detail.conflicts.length)) return '待处理'
  return '待检查'
}
