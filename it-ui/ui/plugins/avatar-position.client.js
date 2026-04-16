const POSITION_PATTERN = /(?:^|[&#])avatar-position=(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)/
const STORAGE_KEY = 'it-avatar-position-map'

function readPositionMap() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

function writePositionMap(map) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(map))
  } catch (e) {
    // Ignore storage failures; inline avatar-position values still work.
  }
}

function clampPosition(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return 50
  return Math.max(0, Math.min(100, Math.round(numberValue)))
}

function getRawSrc(img) {
  return img.getAttribute('src') || img.currentSrc || img.src || ''
}

function stripAvatarPosition(src) {
  const raw = String(src || '')
  let decoded = raw
  try {
    decoded = decodeURIComponent(raw)
  } catch (e) {
    decoded = raw
  }
  return decoded.replace(/#avatar-position=\d+(?:\.\d+)?,\d+(?:\.\d+)?/, '')
}

function normalizeAvatarKey(src) {
  const cleanSrc = stripAvatarPosition(src)
  try {
    const url = new URL(cleanSrc, window.location.origin)
    url.hash = ''
    return url.href
  } catch (e) {
    return cleanSrc
  }
}

function parseAvatarPosition(src) {
  const raw = String(src || '')
  let decoded = raw
  try {
    decoded = decodeURIComponent(raw)
  } catch (e) {
    decoded = raw
  }

  const match = decoded.match(POSITION_PATTERN) || raw.match(POSITION_PATTERN)
  if (!match) {
    return null
  }

  return {
    x: clampPosition(match[1]),
    y: clampPosition(match[2])
  }
}

function resolveAvatarPosition(src) {
  const inlinePosition = parseAvatarPosition(src)
  const key = normalizeAvatarKey(src)

  if (inlinePosition) {
    const map = readPositionMap()
    const value = `${inlinePosition.x},${inlinePosition.y}`
    if (map[key] !== value) {
      map[key] = value
      writePositionMap(map)
    }
    return inlinePosition
  }

  const savedPosition = readPositionMap()[key]
  if (!savedPosition) {
    return null
  }

  const parts = String(savedPosition).split(',')
  if (parts.length !== 2) {
    return null
  }

  return {
    x: clampPosition(parts[0]),
    y: clampPosition(parts[1])
  }
}

function restoreStyle(img, property, value, priority) {
  if (value) {
    img.style.setProperty(property, value, priority || '')
    return
  }

  img.style.removeProperty(property)
}

function rememberStyle(img, property, datasetValueKey, datasetPriorityKey) {
  if (img.dataset[datasetValueKey] !== undefined) {
    return
  }

  img.dataset[datasetValueKey] = img.style.getPropertyValue(property) || ''
  img.dataset[datasetPriorityKey] = img.style.getPropertyPriority(property) || ''
}

function applyStyleIfNeeded(img, property, value, priority = 'important') {
  if (img.style.getPropertyValue(property) === value && img.style.getPropertyPriority(property) === priority) {
    return
  }

  img.style.setProperty(property, value, priority)
}

function restoreAvatarSizing(img) {
  restoreStyle(img, 'display', img.dataset.avatarPositionOriginalDisplay || '', img.dataset.avatarPositionOriginalDisplayPriority || '')
  restoreStyle(img, 'width', img.dataset.avatarPositionOriginalWidth || '', img.dataset.avatarPositionOriginalWidthPriority || '')
  restoreStyle(img, 'height', img.dataset.avatarPositionOriginalHeight || '', img.dataset.avatarPositionOriginalHeightPriority || '')
  delete img.dataset.avatarPositionOriginalDisplay
  delete img.dataset.avatarPositionOriginalDisplayPriority
  delete img.dataset.avatarPositionOriginalWidth
  delete img.dataset.avatarPositionOriginalWidthPriority
  delete img.dataset.avatarPositionOriginalHeight
  delete img.dataset.avatarPositionOriginalHeightPriority
}

function applyElementAvatarSizing(img) {
  if (!img.closest('.el-avatar')) {
    return
  }

  rememberStyle(img, 'display', 'avatarPositionOriginalDisplay', 'avatarPositionOriginalDisplayPriority')
  rememberStyle(img, 'width', 'avatarPositionOriginalWidth', 'avatarPositionOriginalWidthPriority')
  rememberStyle(img, 'height', 'avatarPositionOriginalHeight', 'avatarPositionOriginalHeightPriority')
  applyStyleIfNeeded(img, 'display', 'block')
  applyStyleIfNeeded(img, 'width', '100%')
  applyStyleIfNeeded(img, 'height', '100%')
}

function applyAvatarPosition(img) {
  if (!img || img.nodeType !== 1 || img.tagName !== 'IMG') {
    return
  }

  const position = resolveAvatarPosition(getRawSrc(img))
  if (!position) {
    if (img.dataset.avatarPositionApplied) {
      restoreStyle(img, 'object-fit', img.dataset.avatarPositionOriginalFit || '', img.dataset.avatarPositionOriginalFitPriority || '')
      restoreStyle(img, 'object-position', img.dataset.avatarPositionOriginalPosition || '', img.dataset.avatarPositionOriginalPositionPriority || '')
      img.style.removeProperty('--avatar-position')
      restoreAvatarSizing(img)
      delete img.dataset.avatarPositionApplied
      delete img.dataset.avatarPositionOriginalFit
      delete img.dataset.avatarPositionOriginalFitPriority
      delete img.dataset.avatarPositionOriginalPosition
      delete img.dataset.avatarPositionOriginalPositionPriority
    }
    return
  }

  const value = `${position.x}% ${position.y}%`
  applyElementAvatarSizing(img)
  if (!img.dataset.avatarPositionApplied) {
    img.dataset.avatarPositionOriginalFit = img.style.getPropertyValue('object-fit') || ''
    img.dataset.avatarPositionOriginalFitPriority = img.style.getPropertyPriority('object-fit') || ''
    img.dataset.avatarPositionOriginalPosition = img.style.getPropertyValue('object-position') || ''
    img.dataset.avatarPositionOriginalPositionPriority = img.style.getPropertyPriority('object-position') || ''
  }

  applyStyleIfNeeded(img, 'object-fit', 'cover')
  applyStyleIfNeeded(img, 'object-position', value)
  img.style.setProperty('--avatar-position', value)
  img.dataset.avatarPositionApplied = 'true'

  const avatarRoot = img.closest('.el-avatar')
  if (avatarRoot) {
    avatarRoot.style.setProperty('--avatar-position', value)
  }
}

function applyAvatarPositions(root) {
  const target = root && root.querySelectorAll ? root : document
  target
    .querySelectorAll('.el-avatar img, img[class*="avatar"], img[alt*="avatar"], img[alt*="头像"], img[src*="avatar-position"], img[src*="%23avatar-position"]')
    .forEach(applyAvatarPosition)
}

export default () => {
  if (!process.client || typeof window === 'undefined') {
    return
  }

  let pending = false
  const scheduleApply = () => {
    if (pending) return
    pending = true
    window.requestAnimationFrame(() => {
      pending = false
      applyAvatarPositions(document)
    })
  }

  if (window.__itAvatarPositionObserver) {
    window.__itAvatarPositionObserver.disconnect()
  }

  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === 'attributes' && mutation.target && mutation.target.tagName === 'IMG') {
        applyAvatarPosition(mutation.target)
        continue
      }

      if (mutation.addedNodes && mutation.addedNodes.length) {
        scheduleApply()
      }
    }
  })

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['src', 'srcset', 'style']
  })

  window.__itAvatarPositionObserver = observer

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', scheduleApply, { once: true })
  }

  scheduleApply()
}
