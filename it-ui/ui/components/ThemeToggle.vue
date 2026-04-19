<template>
  <div class="theme-toggle" aria-label="主题选择">
    <el-select
      v-model="theme"
      size="mini"
      class="theme-toggle__select"
      popper-class="theme-toggle-popper"
      placeholder="选择主题"
      @change="setTheme"
    >
      <el-option-group
        v-for="group in themeGroups"
        :key="group.label"
        :label="group.label"
      >
        <el-option
          v-for="item in group.options"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        >
          <div class="theme-option">
            <span class="theme-option__swatch" :class="`is-${item.value}`"></span>
            <div class="theme-option__copy">
              <span class="theme-option__label">{{ item.label }}</span>
              <small class="theme-option__desc">{{ item.description }}</small>
            </div>
          </div>
        </el-option>
      </el-option-group>
    </el-select>
  </div>
</template>

<script>
import { THEME_STORAGE_KEY, THEMES, THEME_GROUPS } from '@/utils/themeRegistry'

export default {
  name: 'ThemeToggle',
  data() {
    return {
      theme: 'classic-light',
      themeGroups: THEME_GROUPS
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

      const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
      return THEMES.includes(savedTheme) ? savedTheme : 'classic-light'
    },
    setTheme(theme) {
      if (this.$theme && typeof this.$theme.set === 'function') {
        const nextState = this.$theme.set(theme)
        this.theme = nextState.theme
        return
      }

      const nextTheme = THEMES.includes(theme) ? theme : 'classic-light'
      const nextMode = nextTheme === 'classic-dark' ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', nextTheme)
      document.documentElement.setAttribute('data-mode', nextMode)
      document.body && document.body.setAttribute('data-theme', nextTheme)
      document.body && document.body.setAttribute('data-mode', nextMode)
      window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
      const detail = { theme: nextTheme, mode: nextMode }
      window.dispatchEvent(new CustomEvent('it-theme-change', { detail }))
      this.theme = nextTheme
    },
    handleStorage(event) {
      if (event.key === THEME_STORAGE_KEY && THEMES.includes(event.newValue)) {
        this.theme = event.newValue
      }
    },
    handleThemeChange(event) {
      if (!event || !event.detail) {
        return
      }
      const nextTheme = typeof event.detail === 'string' ? event.detail : event.detail.theme
      if (THEMES.includes(nextTheme)) {
        this.theme = nextTheme
      }
    }
  }
}
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  min-width: 168px;
}

.theme-toggle__select {
  width: 168px;
}

.theme-toggle :deep(.el-input__inner) {
  height: 38px;
  padding-left: 12px !important;
  border-radius: 12px !important;
  background: var(--it-surface-solid) !important;
  border: 1px solid var(--it-border) !important;
  color: var(--it-text) !important;
  box-shadow: var(--it-shadow-soft, var(--it-shadow)) !important;
}

.theme-toggle :deep(.el-input__icon) {
  line-height: 38px;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0;
}

.theme-option__swatch {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.12);
}

.theme-option__copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.theme-option__label {
  color: var(--it-text);
  font-size: 13px;
  font-weight: 600;
}

.theme-option__desc {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.theme-option__swatch.is-classic-light {
  background: linear-gradient(135deg, #1677ff 0%, #12a594 100%);
}

.theme-option__swatch.is-classic-dark {
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
}

.theme-option__swatch.is-sunny-gold-blue {
  background: linear-gradient(135deg, #F9D77C 0%, #8DA6D2 100%);
}

.theme-option__swatch.is-misty-pink-blue {
  background: linear-gradient(135deg, #F2B6B6 0%, #6A90A6 100%);
}

.theme-option__swatch.is-mint-cyan {
  background: linear-gradient(135deg, #A9D8B7 0%, #7BBFCF 100%);
}

.theme-option__swatch.is-apricot-lavender {
  background: linear-gradient(135deg, #F5B89E 0%, #B8A6D3 100%);
}

@media (max-width: 768px) {
  .theme-toggle,
  .theme-toggle__select {
    width: 144px;
    min-width: 144px;
  }
}

@media (max-width: 520px) {
  .theme-toggle,
  .theme-toggle__select {
    width: 132px;
    min-width: 132px;
  }
}
</style>
