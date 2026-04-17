export const AI_DOCK_MARGIN = 16
export const AI_DOCK_BOTTOM_OFFSET = 104

function clamp(value, min, max) {
  if (max < min) return min
  return Math.min(Math.max(value, min), max)
}

function getBoundSize(viewportSize, dockSize, margin) {
  const safeDockSize = Number.isFinite(dockSize) && dockSize > 0 ? dockSize : 0
  const max = Math.max(margin, viewportSize - safeDockSize - margin)
  return {
    min: margin,
    max
  }
}

export function clampDockPosition({
  x,
  y,
  dockWidth,
  dockHeight,
  viewportWidth,
  viewportHeight,
  margin = AI_DOCK_MARGIN
}) {
  const xBound = getBoundSize(viewportWidth, dockWidth, margin)
  const yBound = getBoundSize(viewportHeight, dockHeight, margin)
  return {
    x: clamp(Number(x) || 0, xBound.min, xBound.max),
    y: clamp(Number(y) || 0, yBound.min, yBound.max)
  }
}

export function snapDockToSide({
  x,
  dockWidth,
  viewportWidth,
  margin = AI_DOCK_MARGIN
}) {
  const safeWidth = Number.isFinite(dockWidth) && dockWidth > 0 ? dockWidth : 0
  const side = (Number(x) || 0) + safeWidth / 2 <= viewportWidth / 2 ? 'left' : 'right'
  const snappedX = side === 'left'
    ? margin
    : Math.max(margin, viewportWidth - safeWidth - margin)
  return {
    side,
    x: snappedX
  }
}

export function resolveDockPosition({
  state,
  dockWidth,
  dockHeight,
  viewportWidth,
  viewportHeight,
  margin = AI_DOCK_MARGIN,
  bottomOffset = AI_DOCK_BOTTOM_OFFSET
}) {
  const safeState = state && typeof state === 'object' ? state : {}
  const rightX = Math.max(margin, viewportWidth - dockWidth - margin)
  const fallbackX = safeState.side === 'left' ? margin : rightX
  const fallbackY = Math.max(margin, viewportHeight - dockHeight - bottomOffset)
  const initialX = Number.isFinite(safeState.x) ? safeState.x : fallbackX
  const initialY = Number.isFinite(safeState.y) ? safeState.y : fallbackY
  return clampDockPosition({
    x: initialX,
    y: initialY,
    dockWidth,
    dockHeight,
    viewportWidth,
    viewportHeight,
    margin
  })
}

