const AI_DOCK_STORAGE_KEY = 'scene_ai_dock_state'

const DEFAULT_DOCK_STATE = {
  side: 'right',
  x: null,
  y: null,
  collapsed: false,
  hidden: false
}

function normalizeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

export function getDefaultAiDockState() {
  return { ...DEFAULT_DOCK_STATE }
}

export function normalizeAiDockState(state) {
  const next = state && typeof state === 'object' ? state : {}
  return {
    side: next.side === 'left' ? 'left' : 'right',
    x: normalizeNumber(next.x),
    y: normalizeNumber(next.y),
    collapsed: Boolean(next.collapsed),
    hidden: Boolean(next.hidden)
  }
}

export function loadAiDockState() {
  if (!process.client) {
    return getDefaultAiDockState()
  }
  try {
    const raw = window.localStorage.getItem(AI_DOCK_STORAGE_KEY)
    if (!raw) {
      return getDefaultAiDockState()
    }
    const parsed = JSON.parse(raw)
    return normalizeAiDockState(parsed)
  } catch (e) {
    return getDefaultAiDockState()
  }
}

export function saveAiDockState(state) {
  const normalized = normalizeAiDockState(state)
  if (!process.client) {
    return normalized
  }
  try {
    window.localStorage.setItem(AI_DOCK_STORAGE_KEY, JSON.stringify(normalized))
  } catch (e) {}
  return normalized
}

