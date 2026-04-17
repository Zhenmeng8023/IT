import { normalizeAiSceneCode } from '@/utils/aiSceneRegistry'

export const ANALYSIS_MODE_VALUES = Object.freeze(['DOC_QA', 'CODE_LOCATE', 'CODE_LOGIC'])
export const LEGACY_ANALYSIS_MODE_STORAGE_KEY = 'ai_assistant_analysis_mode'
export const SCENE_ANALYSIS_MODE_STORAGE_KEY = 'ai_assistant_analysis_mode_by_scene'

function canUseStorage() {
  return typeof window !== 'undefined' && typeof localStorage !== 'undefined'
}

function normalizeMode(mode = '') {
  return String(mode || '').trim().toUpperCase()
}

function getStorageSceneCode(sceneCode = '') {
  return normalizeAiSceneCode(sceneCode) || 'global.assistant'
}

export function isValidAnalysisMode(mode = '') {
  return ANALYSIS_MODE_VALUES.includes(normalizeMode(mode))
}

export function readSceneAnalysisModeMap() {
  if (!canUseStorage()) return {}
  try {
    const raw = localStorage.getItem(SCENE_ANALYSIS_MODE_STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

function writeSceneAnalysisModeMap(nextMap) {
  if (!canUseStorage()) return
  const safeMap = nextMap && typeof nextMap === 'object' ? nextMap : {}
  localStorage.setItem(SCENE_ANALYSIS_MODE_STORAGE_KEY, JSON.stringify(safeMap))
}

export function resolveSceneAnalysisMode(sceneCode = '', defaultMode = 'DOC_QA') {
  const normalizedDefault = isValidAnalysisMode(defaultMode) ? normalizeMode(defaultMode) : 'DOC_QA'
  if (!canUseStorage()) return normalizedDefault

  const key = getStorageSceneCode(sceneCode)
  const modeMap = readSceneAnalysisModeMap()
  const sceneMode = normalizeMode(modeMap[key] || '')
  if (isValidAnalysisMode(sceneMode)) return sceneMode

  const legacyMode = normalizeMode(localStorage.getItem(LEGACY_ANALYSIS_MODE_STORAGE_KEY) || '')
  if (isValidAnalysisMode(legacyMode)) return legacyMode

  return normalizedDefault
}

export function saveSceneAnalysisMode(sceneCode = '', mode = '') {
  if (!canUseStorage()) return
  const normalizedMode = normalizeMode(mode)
  if (!isValidAnalysisMode(normalizedMode)) return

  const key = getStorageSceneCode(sceneCode)
  const nextMap = {
    ...readSceneAnalysisModeMap(),
    [key]: normalizedMode
  }
  writeSceneAnalysisModeMap(nextMap)
  localStorage.setItem(LEGACY_ANALYSIS_MODE_STORAGE_KEY, normalizedMode)
}
