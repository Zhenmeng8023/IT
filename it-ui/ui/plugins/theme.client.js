const STORAGE_KEY = 'it-ui-theme'
const THEMES = ['light', 'dark']

function normalizeTheme(theme) {
  return THEMES.includes(theme) ? theme : 'light'
}

function getInitialTheme() {
  const savedTheme = window.localStorage.getItem(STORAGE_KEY)
  if (THEMES.includes(savedTheme)) {
    return savedTheme
  }

  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

function applyTheme(theme) {
  const nextTheme = normalizeTheme(theme)
  document.documentElement.setAttribute('data-theme', nextTheme)
  document.body && document.body.setAttribute('data-theme', nextTheme)
  window.localStorage.setItem(STORAGE_KEY, nextTheme)
  return nextTheme
}

export default (_, inject) => {
  const initialTheme = applyTheme(getInitialTheme())

  inject('theme', {
    key: STORAGE_KEY,
    current: () => document.documentElement.getAttribute('data-theme') || initialTheme,
    set: (theme) => {
      const nextTheme = applyTheme(theme)
      window.dispatchEvent(new CustomEvent('it-theme-change', { detail: nextTheme }))
      return nextTheme
    }
  })
}
