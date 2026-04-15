const POSITION_PATTERN = /(?:^|[&#])avatar-position=(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)/

function clampPosition(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return 50
  return Math.max(0, Math.min(100, Math.round(numberValue)))
}

function parseAvatarPosition(src) {
  const match = String(src || '').match(POSITION_PATTERN)
  if (!match) {
    return null
  }

  return {
    x: clampPosition(match[1]),
    y: clampPosition(match[2])
  }
}

function applyAvatarPosition(img) {
  if (!img || img.nodeType !== 1 || img.tagName !== 'IMG') {
    return
  }

  const position = parseAvatarPosition(img.getAttribute('src') || img.currentSrc || '')
  if (!position) {
    if (img.dataset.avatarPositionApplied) {
      img.style.objectFit = img.dataset.avatarPositionOriginalFit || ''
      img.style.objectPosition = img.dataset.avatarPositionOriginalPosition || ''
      delete img.dataset.avatarPositionApplied
      delete img.dataset.avatarPositionOriginalFit
      delete img.dataset.avatarPositionOriginalPosition
    }
    return
  }

  const value = `${position.x}% ${position.y}%`
  if (!img.dataset.avatarPositionApplied) {
    img.dataset.avatarPositionOriginalFit = img.style.objectFit || ''
    img.dataset.avatarPositionOriginalPosition = img.style.objectPosition || ''
  }
  img.style.objectFit = 'cover'
  img.style.objectPosition = value
  img.dataset.avatarPositionApplied = 'true'

  const avatarRoot = img.closest('.el-avatar')
  if (avatarRoot) {
    avatarRoot.style.setProperty('--avatar-position', value)
  }
}

function applyAvatarPositions(root) {
  const target = root && root.querySelectorAll ? root : document
  target.querySelectorAll('img[src*="avatar-position="]').forEach(applyAvatarPosition)
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
    attributeFilter: ['src']
  })

  window.__itAvatarPositionObserver = observer

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', scheduleApply, { once: true })
  }

  scheduleApply()
}
