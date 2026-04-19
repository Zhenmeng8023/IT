<template>
  <div v-if="enabled" class="scene-ai-dock" :class="dockClassList" :style="dockStyle">
    <button
      v-if="showRestoreHandle"
      ref="hiddenHandle"
      type="button"
      class="dock-restore-handle"
      @click="restoreFromHidden"
    >
      <i class="el-icon-magic-stick"></i>
      <span>AI</span>
    </button>

    <div v-else-if="showMobilePill" class="mobile-pill">
      <button type="button" class="mobile-pill-main" @click="expandFromPill">
        <i class="el-icon-magic-stick"></i>
        <span>{{ sceneLabel }}</span>
      </button>
      <button type="button" class="mobile-pill-hide dock-control" @click.stop="hideDock" aria-label="隐藏场景 AI">
        <i class="el-icon-close"></i>
      </button>
    </div>

    <div
      v-else
      ref="dockCard"
      class="dock-card"
      :class="{
        'is-collapsed': isCollapsed && !isInlineMode,
        'is-mobile-sheet': isMobileView && !isInlineMode
      }"
    >
      <div
        class="dock-header"
        :class="{ 'is-draggable': canDrag, 'is-dragging': isDragging }"
        @mousedown="onDragStart"
      >
        <div class="dock-title">
          <i class="el-icon-magic-stick"></i>
          <span>场景 AI</span>
        </div>
        <div class="dock-header-right">
          <span class="dock-scene">{{ sceneLabel }}</span>
          <div v-if="!isInlineMode" class="dock-controls">
            <button
              type="button"
              class="dock-control"
              :aria-label="isCollapsed ? '展开' : '收起'"
              @click.stop="toggleCollapsed"
            >
              <i :class="isCollapsed ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
            </button>
            <button
              type="button"
              class="dock-control"
              aria-label="隐藏"
              @click.stop="hideDock"
            >
              <i class="el-icon-close"></i>
            </button>
          </div>
        </div>
      </div>

      <div v-show="!isCollapsed || isInlineMode" class="dock-body">
        <div class="dock-status">
          <el-tag size="mini" effect="plain">{{ isLoggedIn ? '已登录' : '未登录' }}</el-tag>
          <el-tag v-if="knowledgeContextLabel" size="mini" type="success" effect="plain">
            {{ knowledgeContextLabel }}
          </el-tag>
          <el-tag v-if="showDeveloperPanel" size="mini" type="info" effect="plain">
            {{ currentAnalysisMode }}
          </el-tag>
        </div>

        <div class="dock-actions">
          <el-button
            v-for="(item, index) in sceneSuggestions"
            :key="`${item.label}-${item.actionCode}-${index}`"
            :type="index === 0 ? 'primary' : 'default'"
            size="small"
            @click="openAssistantWithSuggestion(item)"
          >
            {{ item.label }}
          </el-button>
        </div>

        <div class="dock-empty-state">
          <div class="dock-empty-state__title">当前场景可做事项</div>
          <ul class="dock-empty-state__list">
            <li v-for="(item, index) in sceneSuggestions" :key="`desc-${item.label}-${index}`">
              {{ item.description || item.label }}
            </li>
          </ul>
        </div>

        <div v-if="showDeveloperPanel" class="dock-debug-panel">
          <div class="dock-debug-title">调试信息</div>
          <pre class="dock-debug-text">{{ JSON.stringify({
            sceneCode: currentSceneCode,
            actionCode: lastOpenDetail.actionCode,
            analysisMode: currentAnalysisMode,
            contextPayloadSummary: lastOpenDetail.contextSummary || '暂无',
            metrics: metricsSnapshot
          }, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCurrentUser, getToken } from '@/utils/auth'
import { loadAiDockState, saveAiDockState } from '@/utils/aiDockState'
import {
  clampDockPosition,
  resolveDockPosition,
  snapDockToSide
} from '@/utils/aiDockPosition'
import { buildBlogWritePrompt, buildProjectDetailPrompt, collectProjectDetailContext, summarizeAiContextPayload } from '@/utils/aiContextCollectors'
import { getAiSuggestionPreset } from '@/utils/aiSuggestionPreset'
import { getAiUxMetricsSnapshot, recordAiUxMetric } from '@/utils/aiUxMetrics'
import { resolveSceneAnalysisMode } from '@/utils/aiModePreference'
import { normalizeAiSceneCode, resolveAiSceneMeta } from '@/utils/aiSceneRegistry'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'
const SELECTED_KBS_STORAGE_KEY = 'ai_assistant_selected_kb_ids'

const MOBILE_BREAKPOINT = 768

export default {
  name: 'SceneAiDock',
  props: {
    scene: {
      type: String,
      default: ''
    },
    project: {
      type: Object,
      default: () => ({})
    },
    blog: {
      type: Object,
      default: () => ({})
    },
    inline: {
      type: Boolean,
      default: false
    },
    enabled: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      dockState: {
        side: 'right',
        x: null,
        y: null,
        collapsed: false,
        hidden: false
      },
      position: {
        x: 0,
        y: 0
      },
      isMobileView: false,
      isDragging: false,
      dragContext: null,
      hasDockInitialized: false,
      lastOpenDetail: {
        sceneCode: '',
        actionCode: '',
        contextSummary: ''
      },
      metricVersion: 0
    }
  },
  computed: {
    currentSceneCode() {
      if (this.scene) return normalizeAiSceneCode(this.scene)
      const path = (this.$route && this.$route.path) || ''
      if (path.includes('/projectdetail')) return 'project.detail'
      if (path.includes('/blogwrite')) return 'blog.write'
      if (path.includes('/knowledge-base')) return 'knowledge.base'
      return 'global.assistant'
    },
    sceneMeta() {
      return resolveAiSceneMeta({
        route: this.$route,
        sceneCode: this.currentSceneCode
      })
    },
    sceneLabel() {
      if (this.currentSceneCode === 'project.detail') return '项目详情'
      if (this.currentSceneCode === 'blog.write') return '博客写作'
      if (this.currentSceneCode === 'knowledge.base') return '知识库'
      return '通用场景'
    },
    isLoggedIn() {
      return Boolean(getToken() || getCurrentUser())
    },
    knowledgeContextLabel() {
      const ids = this.readKnowledgeBaseIds()
      if (!ids.length) return ''
      return ids.length === 1 ? `知识库 #${ids[0]}` : `${ids.length} 个知识库`
    },
    isInlineMode() {
      return Boolean(this.inline)
    },
    isHidden() {
      return !this.isInlineMode && this.dockState.hidden
    },
    isCollapsed() {
      return !this.isInlineMode && this.dockState.collapsed
    },
    canDrag() {
      return !this.isInlineMode && !this.isMobileView && !this.isHidden
    },
    showRestoreHandle() {
      return this.isHidden
    },
    showMobilePill() {
      return this.isMobileView && !this.isHidden && this.isCollapsed
    },
    sceneSuggestions() {
      return getAiSuggestionPreset(this.currentSceneCode).slice(0, 5)
    },
    currentAnalysisMode() {
      return resolveSceneAnalysisMode(this.currentSceneCode, this.sceneMeta.defaultAnalysisMode || 'DOC_QA')
    },
    debugUiEnabled() {
      if (process.env.NODE_ENV !== 'production') return true
      if (!process.client) return false
      try {
        const query = this.$route && this.$route.query ? this.$route.query : {}
        return localStorage.getItem('ai_assistant_force_debug') === '1' || query.aiDebug === '1' || window.__AI_ASSISTANT_DEBUG__ === true
      } catch (e) {
        return false
      }
    },
    showDeveloperPanel() {
      return this.debugUiEnabled
    },
    metricsSnapshot() {
      void this.metricVersion
      return getAiUxMetricsSnapshot()
    },
    dockClassList() {
      return {
        'is-inline': this.isInlineMode,
        'is-mobile': this.isMobileView && !this.isInlineMode,
        'is-hidden': this.isHidden,
        'is-side-left': !this.isInlineMode && this.dockState.side === 'left',
        'is-side-right': !this.isInlineMode && this.dockState.side !== 'left'
      }
    },
    dockStyle() {
      if (this.isInlineMode || this.isMobileView) {
        return {}
      }
      if (this.isHidden) {
        return this.dockState.side === 'left'
          ? { top: `${this.position.y}px`, left: '0px' }
          : { top: `${this.position.y}px`, right: '0px' }
      }
      return {
        top: `${this.position.y}px`,
        left: `${this.position.x}px`
      }
    }
  },
  watch: {
    enabled(val) {
      if (!val || !this.hasDockInitialized || this.isInlineMode || !process.client) return
      this.$nextTick(() => {
        this.syncDockPosition(false)
      })
    },
    inline(val) {
      if (!process.client) return
      if (val) {
        window.removeEventListener('resize', this.handleWindowResize)
        this.clearDragging()
        return
      }
      this.initializeDock()
    },
    isCollapsed() {
      if (this.isInlineMode || this.isMobileView || !process.client) return
      this.$nextTick(() => {
        this.syncDockPosition(false)
      })
    }
  },
  mounted() {
    this.initializeDock()
    if (process.client) {
      window.addEventListener('ai-ux-metric', this.handleUxMetricEvent)
    }
  },
  beforeDestroy() {
    if (!process.client) return
    window.removeEventListener('ai-ux-metric', this.handleUxMetricEvent)
    window.removeEventListener('resize', this.handleWindowResize)
    this.clearDragging()
  },
  methods: {
    handleUxMetricEvent() {
      this.metricVersion += 1
    },
    markDockExpanded(source = 'dock') {
      recordAiUxMetric('dockExpand', {
        sceneCode: this.currentSceneCode,
        source
      })
    },
    initializeDock() {
      if (!process.client || this.isInlineMode) return
      this.isMobileView = this.detectMobileView()
      this.dockState = loadAiDockState()
      this.hasDockInitialized = true
      if (!this.isMobileView) {
        this.$nextTick(() => {
          this.syncDockPosition(true)
        })
      }
      window.removeEventListener('resize', this.handleWindowResize)
      window.addEventListener('resize', this.handleWindowResize)
    },

    detectMobileView() {
      return window.innerWidth <= MOBILE_BREAKPOINT
    },

    getDockSize() {
      const fallback = this.isHidden
        ? { width: 44, height: 44 }
        : { width: 228, height: this.isCollapsed ? 56 : 240 }
      const element = this.isHidden ? this.$refs.hiddenHandle : this.$refs.dockCard
      if (!element || typeof element.getBoundingClientRect !== 'function') {
        return fallback
      }
      const rect = element.getBoundingClientRect()
      return {
        width: Math.max(rect.width, fallback.width),
        height: Math.max(rect.height, fallback.height)
      }
    },

    syncDockPosition(useSavedState) {
      if (!process.client || this.isInlineMode || this.isMobileView) return
      const size = this.getDockSize()
      const state = useSavedState
        ? this.dockState
        : {
            ...this.dockState,
            x: this.position.x,
            y: this.position.y
          }
      const next = resolveDockPosition({
        state,
        dockWidth: size.width,
        dockHeight: size.height,
        viewportWidth: window.innerWidth,
        viewportHeight: window.innerHeight
      })
      this.position = next
      this.persistDockState({
        x: next.x,
        y: next.y
      })
    },

    persistDockState(patch) {
      this.dockState = saveAiDockState({
        ...this.dockState,
        ...patch
      })
    },

    handleWindowResize() {
      if (!process.client || this.isInlineMode) return
      const wasMobile = this.isMobileView
      this.isMobileView = this.detectMobileView()
      if (this.isMobileView) {
        this.clearDragging()
        return
      }
      this.$nextTick(() => {
        this.syncDockPosition(wasMobile)
      })
    },

    onDragStart(event) {
      if (!this.canDrag || !event || event.button !== 0) return
      if (event.target && event.target.closest && event.target.closest('.dock-control')) return
      const card = this.$refs.dockCard
      if (!card || typeof card.getBoundingClientRect !== 'function') return

      const rect = card.getBoundingClientRect()
      this.dragContext = {
        offsetX: event.clientX - rect.left,
        offsetY: event.clientY - rect.top,
        width: rect.width,
        height: rect.height
      }
      this.position = {
        x: rect.left,
        y: rect.top
      }
      this.isDragging = true
      window.addEventListener('mousemove', this.onDragMove)
      window.addEventListener('mouseup', this.onDragEnd)
    },

    onDragMove(event) {
      if (!this.isDragging || !this.dragContext || !process.client) return
      const next = clampDockPosition({
        x: event.clientX - this.dragContext.offsetX,
        y: event.clientY - this.dragContext.offsetY,
        dockWidth: this.dragContext.width,
        dockHeight: this.dragContext.height,
        viewportWidth: window.innerWidth,
        viewportHeight: window.innerHeight
      })
      this.position = next
    },

    onDragEnd() {
      if (!this.isDragging || !this.dragContext || !process.client) {
        this.clearDragging()
        return
      }
      const snapped = snapDockToSide({
        x: this.position.x,
        dockWidth: this.dragContext.width,
        viewportWidth: window.innerWidth
      })
      const next = clampDockPosition({
        x: snapped.x,
        y: this.position.y,
        dockWidth: this.dragContext.width,
        dockHeight: this.dragContext.height,
        viewportWidth: window.innerWidth,
        viewportHeight: window.innerHeight
      })
      this.position = next
      this.persistDockState({
        side: snapped.side,
        x: next.x,
        y: next.y
      })
      this.clearDragging()
    },

    clearDragging() {
      this.isDragging = false
      this.dragContext = null
      if (!process.client) return
      window.removeEventListener('mousemove', this.onDragMove)
      window.removeEventListener('mouseup', this.onDragEnd)
    },

    toggleCollapsed() {
      if (this.isInlineMode) return
      const nextCollapsed = !this.isCollapsed
      this.persistDockState({
        collapsed: nextCollapsed,
        hidden: false
      })
      if (!nextCollapsed) {
        this.markDockExpanded('toggle-collapsed')
      }
      if (!this.isMobileView) {
        this.$nextTick(() => {
          this.syncDockPosition(false)
        })
      }
    },

    expandFromPill() {
      this.persistDockState({
        collapsed: false,
        hidden: false
      })
      this.markDockExpanded('mobile-pill')
    },

    hideDock() {
      if (this.isInlineMode) return
      this.persistDockState({
        hidden: true
      })
      if (!this.isMobileView) {
        this.$nextTick(() => {
          this.syncDockPosition(false)
        })
      }
    },

    restoreFromHidden() {
      if (this.isInlineMode) return
      if (this.isMobileView) {
        this.persistDockState({
          hidden: false,
          collapsed: true
        })
        this.markDockExpanded('restore-hidden-mobile')
        return
      }
      this.persistDockState({
        hidden: false
      })
      this.markDockExpanded('restore-hidden')
      this.$nextTick(() => {
        this.syncDockPosition(false)
      })
    },

    stripHtml(html) {
      return String(html || '')
        .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
        .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
        .replace(/<[^>]+>/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    },

    readKnowledgeBaseIds() {
      if (!process.client) return []
      if (this.currentSceneCode !== 'knowledge.base') return []
      try {
        const rawList = localStorage.getItem(SELECTED_KBS_STORAGE_KEY)
        if (rawList) {
          const parsed = JSON.parse(rawList)
          if (Array.isArray(parsed)) return parsed.map(item => Number(item) || null).filter(Boolean)
        }
      } catch (e) {}

      const ids = []
      const selected = Number(localStorage.getItem(SELECTED_KB_STORAGE_KEY) || 0) || null
      if (selected) ids.push(selected)
      try {
        const sceneKb = JSON.parse(localStorage.getItem(CURRENT_KB_STORAGE_KEY) || 'null')
        if (sceneKb && sceneKb.id && !ids.includes(Number(sceneKb.id))) ids.push(Number(sceneKb.id))
      } catch (e) {}
      return ids
    },

    buildFallbackContextPayload() {
      if (this.currentSceneCode === 'project.detail') {
        return collectProjectDetailContext({ project: this.project })
      }
      return {}
    },

    collectContextPayload(actionCode = '') {
      if (this.$aiActionBridge && typeof this.$aiActionBridge.collectContext === 'function') {
        const collected = this.$aiActionBridge.collectContext(this.currentSceneCode, { actionCode })
        if (collected) return collected
      }
      return this.buildFallbackContextPayload()
    },

    buildPrompt(actionCode, contextPayload = {}, suggestion = null) {
      const pageTitle = process.client && document && document.title ? document.title : '当前页面'
      const path = (this.$route && this.$route.fullPath) || '/'

      if (this.currentSceneCode === 'project.detail') {
        return buildProjectDetailPrompt(actionCode, contextPayload)
      }

      if (this.currentSceneCode === 'blog.write') {
        return buildBlogWritePrompt(actionCode, contextPayload)
      }

      if (suggestion && suggestion.prompt) {
        return suggestion.prompt
      }

      return `请分析当前页面能做什么，并给我下一步操作建议。\n\n页面标题：${pageTitle}\n路由：${path}`
    },

    openAssistantWithSuggestion(suggestion = {}) {
      const actionCode = String(suggestion.actionCode || '').trim()
      const contextPayload = this.collectContextPayload(actionCode)
      const knowledgeBaseIds = this.readKnowledgeBaseIds()
      const detail = {
        prompt: this.buildPrompt(actionCode, contextPayload, suggestion),
        autoSend: true,
        source: 'scene-ai-dock',
        sceneCode: this.currentSceneCode,
        actionCode,
        scene: this.currentSceneCode,
        action: actionCode,
        contextPayload
      }
      if (knowledgeBaseIds.length) detail.knowledgeBaseIds = knowledgeBaseIds

      this.lastOpenDetail = {
        sceneCode: this.currentSceneCode,
        actionCode,
        contextSummary: summarizeAiContextPayload(contextPayload)
      }
      recordAiUxMetric('sceneActionClick', {
        sceneCode: this.currentSceneCode,
        actionCode,
        label: suggestion.label || ''
      })

      if (this.$aiActionBridge && typeof this.$aiActionBridge.open === 'function') {
        this.$aiActionBridge.open(detail)
      } else if (process.client) {
        window.dispatchEvent(new CustomEvent('ai-assistant-open', { detail }))
      }
      this.$emit('quick-analysis-opened', detail)
    }
  }
}
</script>

<style scoped>
.scene-ai-dock {
  position: fixed;
  z-index: 1050;
  width: 228px;
}

.scene-ai-dock.is-inline {
  position: static;
  width: 100%;
}

.scene-ai-dock.is-mobile {
  left: 12px;
  right: 12px;
  width: auto;
  bottom: 88px;
}

.scene-ai-dock.is-mobile.is-hidden {
  left: auto;
  right: 12px;
  width: 44px;
}

.scene-ai-dock.is-side-left.is-hidden,
.scene-ai-dock.is-side-right.is-hidden {
  width: auto;
}

.dock-card {
  background: var(--it-surface-elevated, var(--it-surface-solid));
  border: 1px solid var(--it-border);
  border-radius: 8px;
  box-shadow: var(--it-shadow-strong, var(--it-shadow));
  backdrop-filter: blur(18px);
  padding: 12px;
}

.dock-card.is-collapsed {
  padding-bottom: 10px;
}

.dock-card.is-mobile-sheet {
  max-height: min(62vh, 420px);
  overflow-y: auto;
}

.dock-header,
.dock-title,
.dock-status,
.dock-header-right,
.dock-controls {
  display: flex;
  align-items: center;
}

.dock-header {
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.dock-header.is-draggable {
  cursor: move;
  user-select: none;
}

.dock-header.is-dragging {
  cursor: grabbing;
}

.dock-title {
  gap: 8px;
  color: var(--it-text);
  font-weight: 700;
  font-size: 14px;
}

.dock-title i {
  color: var(--it-accent);
  font-size: 16px;
}

.dock-header-right {
  gap: 8px;
}

.dock-scene {
  font-size: 12px;
  color: var(--it-text-subtle);
  white-space: nowrap;
}

.dock-controls {
  gap: 4px;
}

.dock-control {
  width: 24px;
  height: 24px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text-subtle);
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all .2s ease;
}

.dock-control:hover {
  color: var(--it-accent);
  border-color: var(--it-accent);
}

.dock-body {
  display: grid;
  gap: 10px;
}

.dock-status {
  flex-wrap: wrap;
  gap: 6px;
}

.dock-actions {
  display: grid;
  gap: 8px;
}

.dock-actions .el-button {
  width: 100%;
  border-radius: 8px;
}

.dock-empty-state {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--it-fill-soft);
  border: 1px dashed var(--it-border);
}

.dock-empty-state__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--it-text-primary);
}

.dock-empty-state__list {
  margin: 8px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
  color: var(--it-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.dock-debug-panel {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--it-contrast-panel-bg);
  color: var(--it-contrast-text);
}

.dock-debug-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--it-contrast-text-muted);
}

.dock-debug-text {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 11px;
  line-height: 1.5;
}

.dock-restore-handle {
  width: 44px;
  height: 44px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-elevated, var(--it-surface-solid));
  border-radius: 8px;
  box-shadow: var(--it-shadow);
  color: var(--it-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

.mobile-pill {
  width: max-content;
  margin: 0 auto;
  display: flex;
  align-items: center;
  background: var(--it-surface-elevated, var(--it-surface-solid));
  border: 1px solid var(--it-border);
  border-radius: 8px;
  box-shadow: var(--it-shadow);
  overflow: hidden;
}

.mobile-pill-main {
  border: none;
  background: transparent;
  color: var(--it-text);
  height: 38px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.mobile-pill-hide {
  border-radius: 0;
  border: none;
  border-left: 1px solid var(--it-border);
  height: 38px;
  width: 38px;
}

@media (max-width: 768px) {
  .scene-ai-dock {
    width: auto;
  }

  .dock-header {
    margin-bottom: 8px;
  }
}
</style>
