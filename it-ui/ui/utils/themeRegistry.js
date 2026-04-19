export const THEME_STORAGE_KEY = 'it-ui-theme'
export const THEME_ATTR = 'data-theme'
export const MODE_ATTR = 'data-mode'

export const THEME_GROUPS = [
  {
    label: '经典主题',
    options: [
      {
        value: 'classic-light',
        label: '经典亮色',
        description: '保留当前默认浅色风格',
        mode: 'light'
      },
      {
        value: 'classic-dark',
        label: '经典暗色',
        description: '保留当前原有暗色主题',
        mode: 'dark'
      }
    ]
  },
  {
    label: '柔和配色主题',
    options: [
      {
        value: 'sunny-gold-blue',
        label: '暖阳金蓝',
        description: '奶油金 + 柔和雾蓝',
        mode: 'light'
      },
      {
        value: 'misty-pink-blue',
        label: '柔雾粉蓝',
        description: '奶油粉 + 灰雾蓝',
        mode: 'light'
      },
      {
        value: 'mint-cyan',
        label: '薄荷青潮',
        description: '薄荷绿 + 青蓝',
        mode: 'light'
      },
      {
        value: 'apricot-lavender',
        label: '杏桃紫雾',
        description: '杏桃橘 + 浅雾紫',
        mode: 'light'
      }
    ]
  }
]

export const THEME_OPTIONS = THEME_GROUPS.reduce((list, group) => list.concat(group.options), [])
export const THEMES = THEME_OPTIONS.map(item => item.value)
export const DEFAULT_THEME = 'classic-light'
export const DARK_THEMES = THEME_OPTIONS.filter(item => item.mode === 'dark').map(item => item.value)

export function getThemeMeta(theme) {
  return THEME_OPTIONS.find(item => item.value === theme) || THEME_OPTIONS[0]
}

export function resolveMode(theme) {
  return DARK_THEMES.includes(theme) ? 'dark' : 'light'
}

export function normalizeTheme(theme) {
  return THEMES.includes(theme) ? theme : DEFAULT_THEME
}

export function getFallbackTheme(prefersDark = false) {
  return prefersDark ? 'classic-dark' : DEFAULT_THEME
}

export function getInitialTheme() {
  const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
  if (THEMES.includes(savedTheme)) {
    return savedTheme
  }

  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  return getFallbackTheme(prefersDark)
}

export function applyTheme(theme) {
  const nextTheme = normalizeTheme(theme)
  const nextMode = resolveMode(nextTheme)

  document.documentElement.setAttribute(THEME_ATTR, nextTheme)
  document.documentElement.setAttribute(MODE_ATTR, nextMode)

  if (document.body) {
    document.body.setAttribute(THEME_ATTR, nextTheme)
    document.body.setAttribute(MODE_ATTR, nextMode)
  }

  window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
  return {
    theme: nextTheme,
    mode: nextMode,
    meta: getThemeMeta(nextTheme)
  }
}
