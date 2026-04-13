<template>
  <div class="theme-toggle" aria-label="主题选择">
    <el-radio-group v-model="theme" size="mini" @change="setTheme">
      <el-radio-button label="light">
        <i class="el-icon-sunny"></i>
        <span>亮色</span>
      </el-radio-button>
      <el-radio-button label="dark">
        <i class="el-icon-moon"></i>
        <span>暗色</span>
      </el-radio-button>
    </el-radio-group>
  </div>
</template>

<script>
const STORAGE_KEY = 'it-ui-theme'
const THEMES = ['light', 'dark']

export default {
  name: 'ThemeToggle',
  data() {
    return {
      theme: 'light'
    }
  },
  mounted() {
    this.theme = this.getCurrentTheme()
    window.addEventListener('storage', this.handleStorage)
    window.addEventListener('it-theme-change', this.handleThemeChange)
  },
  beforeDestroy() {
    window.removeEventListener('storage', this.handleStorage)
    window.removeEventListener('it-theme-change', this.handleThemeChange)
  },
  methods: {
    getCurrentTheme() {
      const domTheme = document.documentElement.getAttribute('data-theme')
      if (THEMES.includes(domTheme)) {
        return domTheme
      }

      const savedTheme = window.localStorage.getItem(STORAGE_KEY)
      return THEMES.includes(savedTheme) ? savedTheme : 'light'
    },
    setTheme(theme) {
      if (this.$theme && typeof this.$theme.set === 'function') {
        this.theme = this.$theme.set(theme)
        return
      }

      const nextTheme = THEMES.includes(theme) ? theme : 'light'
      document.documentElement.setAttribute('data-theme', nextTheme)
      document.body && document.body.setAttribute('data-theme', nextTheme)
      window.localStorage.setItem(STORAGE_KEY, nextTheme)
      window.dispatchEvent(new CustomEvent('it-theme-change', { detail: nextTheme }))
      this.theme = nextTheme
    },
    handleStorage(event) {
      if (event.key === STORAGE_KEY && THEMES.includes(event.newValue)) {
        this.theme = event.newValue
      }
    },
    handleThemeChange(event) {
      if (THEMES.includes(event.detail)) {
        this.theme = event.detail
      }
    }
  }
}
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
}

.theme-toggle :deep(.el-radio-group) {
  display: inline-flex;
  padding: 3px;
  border: 1px solid var(--it-border);
  border-radius: var(--it-radius-control);
  background: var(--it-surface-muted);
}

.theme-toggle :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: 0 !important;
  border-radius: 6px !important;
  background: transparent;
  color: var(--it-text-muted);
  font-weight: 600;
  box-shadow: none !important;
}

.theme-toggle :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: var(--it-surface-solid);
  color: var(--it-accent);
  box-shadow: var(--it-shadow) !important;
}

@media (max-width: 520px) {
  .theme-toggle span {
    display: none;
  }

  .theme-toggle :deep(.el-radio-button__inner) {
    width: 30px;
    justify-content: center;
    padding: 0;
  }
}
</style>
