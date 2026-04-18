const AI_UX_METRIC_STATE = {
  dockExpandCount: 0,
  sceneActionClickCount: 0,
  applySuccessCount: 0,
  manualModeSwitchCount: 0
}

function canUseWindow() {
  return typeof window !== 'undefined'
}

function cloneMetricState() {
  return {
    dockExpandCount: AI_UX_METRIC_STATE.dockExpandCount,
    sceneActionClickCount: AI_UX_METRIC_STATE.sceneActionClickCount,
    applySuccessCount: AI_UX_METRIC_STATE.applySuccessCount,
    manualModeSwitchCount: AI_UX_METRIC_STATE.manualModeSwitchCount
  }
}

function shouldConsoleLog() {
  if (process.env.NODE_ENV !== 'production') return true
  if (!canUseWindow()) return false
  try {
    return window.localStorage.getItem('ai_assistant_force_debug') === '1'
  } catch (e) {
    return false
  }
}

export function getAiUxMetricsSnapshot() {
  return cloneMetricState()
}

export function recordAiUxMetric(type = '', payload = {}) {
  const metricType = String(type || '').trim()
  if (!metricType) return cloneMetricState()

  if (metricType === 'dockExpand') AI_UX_METRIC_STATE.dockExpandCount += 1
  if (metricType === 'sceneActionClick') AI_UX_METRIC_STATE.sceneActionClickCount += 1
  if (metricType === 'applySuccess') AI_UX_METRIC_STATE.applySuccessCount += 1
  if (metricType === 'manualModeSwitch') AI_UX_METRIC_STATE.manualModeSwitchCount += 1

  const snapshot = cloneMetricState()
  if (shouldConsoleLog()) {
    console.info('[AI UX Metric]', metricType, {
      ...payload,
      snapshot
    })
  }

  if (canUseWindow()) {
    window.dispatchEvent(new CustomEvent('ai-ux-metric', {
      detail: {
        type: metricType,
        payload,
        snapshot
      }
    }))
  }

  return snapshot
}

export function resetAiUxMetrics() {
  AI_UX_METRIC_STATE.dockExpandCount = 0
  AI_UX_METRIC_STATE.sceneActionClickCount = 0
  AI_UX_METRIC_STATE.applySuccessCount = 0
  AI_UX_METRIC_STATE.manualModeSwitchCount = 0
}
