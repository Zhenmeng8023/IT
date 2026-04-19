import {
  THEME_STORAGE_KEY,
  THEME_GROUPS,
  applyTheme,
  getInitialTheme,
  getThemeMeta,
  resolveMode
} from '@/utils/themeRegistry'

export default (_, inject) => {
  const initialState = applyTheme(getInitialTheme())

  inject('theme', {
    key: THEME_STORAGE_KEY,
    groups: THEME_GROUPS,
    current: () => document.documentElement.getAttribute('data-theme') || initialState.theme,
    mode: () => document.documentElement.getAttribute('data-mode') || initialState.mode,
    meta: (theme) => getThemeMeta(theme || (document.documentElement.getAttribute('data-theme') || initialState.theme)),
    resolveMode,
    set: (theme) => {
      const nextState = applyTheme(theme)
      window.dispatchEvent(new CustomEvent('it-theme-change', { detail: nextState }))
      return nextState
    }
  })
}
