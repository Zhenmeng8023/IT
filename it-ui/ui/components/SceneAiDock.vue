<template>
  <div v-if="enabled" class="scene-ai-dock" :class="{ 'is-inline': inline }">
    <div class="dock-card">
      <div class="dock-header">
        <div class="dock-title">
          <i class="el-icon-magic-stick"></i>
          <span>场景 AI</span>
        </div>
        <span class="dock-scene">{{ sceneLabel }}</span>
      </div>

      <div class="dock-status">
        <el-tag size="mini" effect="plain">{{ isLoggedIn ? '已登录' : '未登录' }}</el-tag>
        <el-tag v-if="knowledgeContextLabel" size="mini" type="success" effect="plain">
          {{ knowledgeContextLabel }}
        </el-tag>
      </div>

      <div class="dock-actions">
        <template v-if="currentScene === 'project-detail'">
          <el-button type="primary" size="small" icon="el-icon-document" @click="openAssistantWithPrompt('project-summary')">
            分析项目
          </el-button>
          <el-button size="small" icon="el-icon-s-operation" @click="openAssistantWithPrompt('project-tasks')">
            拆解任务
          </el-button>
        </template>

        <template v-else-if="currentScene === 'blog-write'">
          <el-button type="primary" size="small" icon="el-icon-edit-outline" @click="openAssistantWithPrompt('blog-polish')">
            润色正文
          </el-button>
          <el-button size="small" icon="el-icon-document-copy" @click="openAssistantWithPrompt('blog-summary')">
            摘要标签
          </el-button>
        </template>

        <template v-else>
          <el-button type="primary" size="small" icon="el-icon-chat-dot-round" @click="openAssistantWithPrompt('general')">
            分析当前页面
          </el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { buildProjectAiPayload } from '@/api/aiAssistant'
import { getCurrentUser, getToken } from '@/utils/auth'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'
const SELECTED_KBS_STORAGE_KEY = 'ai_assistant_selected_kb_ids'

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
  computed: {
    currentScene() {
      if (this.scene) return this.scene
      const path = (this.$route && this.$route.path) || ''
      if (path.includes('/projectdetail')) return 'project-detail'
      if (path.includes('/blogwrite')) return 'blog-write'
      return 'general'
    },
    sceneLabel() {
      if (this.currentScene === 'project-detail') return '项目详情'
      if (this.currentScene === 'blog-write') return '博客写作'
      return '通用场景'
    },
    isLoggedIn() {
      return Boolean(getToken() || getCurrentUser())
    },
    knowledgeContextLabel() {
      const ids = this.readKnowledgeBaseIds()
      if (!ids.length) return ''
      return ids.length === 1 ? `知识库 #${ids[0]}` : `${ids.length} 个知识库`
    }
  },
  methods: {
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

    buildPrompt(action) {
      const pageTitle = process.client && document && document.title ? document.title : '当前页面'
      const path = (this.$route && this.$route.fullPath) || '/'

      if (action === 'project-summary') {
        const title = this.project.title || this.project.projectName || this.project.name || '未命名项目'
        return `请基于当前项目上下文做自然语言分析，说明项目目标、核心功能、风险和下一步建议。\n\n项目：${title}\n${buildProjectAiPayload(this.project)}`
      }

      if (action === 'project-tasks') {
        const title = this.project.title || this.project.projectName || this.project.name || '未命名项目'
        return `请把当前项目拆成可执行任务，按阶段给出优先级、交付物和注意事项。\n\n项目：${title}\n${buildProjectAiPayload(this.project)}`
      }

      if (action === 'blog-polish') {
        const title = this.blog.title || '未命名博客'
        const content = this.stripHtml(this.blog.content || '')
        return `请润色下面这篇博客，保留原意，不编造事实，并指出需要作者确认的地方。\n\n标题：${title}\n正文：\n${content || '当前正文为空，请先给出写作建议。'}`
      }

      if (action === 'blog-summary') {
        const title = this.blog.title || '未命名博客'
        const content = this.stripHtml(this.blog.content || '')
        return `请为下面这篇博客生成 120 字以内摘要，并给出 3-5 个具体标签。\n\n标题：${title}\n正文：\n${content || '当前正文为空，请先给出摘要写作建议。'}`
      }

      return `请分析当前页面能做什么，并给我下一步操作建议。\n\n页面标题：${pageTitle}\n路由：${path}`
    },

    openAssistantWithPrompt(action) {
      const detail = {
        prompt: this.buildPrompt(action),
        autoSend: true,
        source: 'scene-ai-dock',
        scene: this.currentScene,
        action,
        knowledgeBaseIds: this.readKnowledgeBaseIds()
      }

      if (process.client) {
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
  right: 24px;
  bottom: 104px;
  z-index: 1050;
  width: 228px;
}

.scene-ai-dock.is-inline {
  position: static;
  width: 100%;
}

.dock-card {
  background: var(--it-surface-elevated, var(--it-surface-solid));
  border: 1px solid var(--it-border);
  border-radius: 8px;
  box-shadow: var(--it-shadow-strong, var(--it-shadow));
  backdrop-filter: blur(18px);
  padding: 12px;
}

.dock-header,
.dock-title,
.dock-status {
  display: flex;
  align-items: center;
}

.dock-header {
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
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

.dock-scene {
  font-size: 12px;
  color: var(--it-text-subtle);
  white-space: nowrap;
}

.dock-status {
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.dock-actions {
  display: grid;
  gap: 8px;
}

.dock-actions .el-button {
  width: 100%;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .scene-ai-dock {
    right: 12px;
    left: 12px;
    width: auto;
    bottom: 88px;
  }
}
</style>
