<template>
  <div class="global-ai-assistant">
    <transition name="ai-float">
      <div v-if="!visible" class="ai-fab" @click="openDrawer">
        <i class="el-icon-chat-dot-round"></i>
        <span>AI 助手</span>
      </div>
    </transition>

    <el-drawer
      :visible.sync="visible"
      direction="rtl"
      :size="drawerWidth + 'px'"
      :with-header="false"
      append-to-body
      custom-class="ai-drawer"
    >
      <div class="ai-panel" :style="{ '--chat-body-height': chatBodyHeight + 'px' }">
        <div class="ai-resize-handle ai-resize-handle--x" @mousedown="startDrawerResize"></div>
        <div class="ai-panel__header">
          <div class="ai-panel__title-wrap">
            <div class="ai-panel__title">全局 AI 助手</div>
            <div class="ai-panel__subtitle">通用问答、业务导航、上下文提示</div>
          </div>
          <el-button type="text" icon="el-icon-close" @click="visible = false" />
        </div>

        <div class="ai-panel__scroll">
          <div class="ai-panel__scene">
            <el-tag size="mini" effect="plain">{{ sceneLabel }}</el-tag>
            <el-tag v-if="selectedKnowledgeBaseLabel" size="mini" type="success" effect="plain">
              知识库：{{ selectedKnowledgeBaseLabel }}
            </el-tag>
            <el-tag v-if="displayModelName" size="mini" type="warning" effect="plain">
              当前模型：{{ displayModelName }}
            </el-tag>
            <el-tag v-if="isLoggedIn" size="mini" type="info" effect="plain">
              已登录
            </el-tag>
            <el-tag v-else size="mini" type="danger" effect="plain">
              未检测到登录态
            </el-tag>
          </div>

          <div class="ai-card ai-panel__nav">
            <div class="section-title">快捷导航</div>
            <div class="nav-grid">
              <div
                v-for="item in quickNavs"
                :key="item.path"
                class="nav-card"
                :class="{ 'nav-card--disabled': item.disabled }"
                @click="go(item.path, item.disabled)"
              >
                <div class="nav-card__icon">
                  <i :class="item.icon"></i>
                </div>
                <div class="nav-card__text">
                  <div class="nav-card__label">{{ item.label }}</div>
                  <div class="nav-card__desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="ai-card ai-panel__model">
            <div class="section-title">对话模型</div>
            <el-select
              v-model="selectedModelId"
              clearable
              filterable
              size="small"
              placeholder="选择本次对话使用的模型"
              class="full-width-select"
              no-data-text="暂无可选模型"
              @change="handleModelChange"
            >
              <el-option
                v-for="item in modelOptions"
                :key="item.id"
                :label="item.modelName"
                :value="item.id"
              >
                <div class="ai-model-option">
                  <div class="ai-model-option__left">
                    <span class="ai-model-option__name">{{ item.modelName }}</span>
                    <span v-if="item.isActive" class="ai-model-option__badge">当前</span>
                  </div>
                  <span class="ai-model-option__meta">{{ item.modelType || item.providerCode || '' }}</span>
                </div>
              </el-option>
            </el-select>
          </div>

          <div class="ai-card ai-panel__kb">
            <div class="section-title">关联知识库（可选）</div>
            <el-select
              v-model="selectedKnowledgeBaseId"
              clearable
              filterable
              size="small"
              placeholder="选择知识库增强回答"
              class="full-width-select"
              no-data-text="暂无可选知识库"
              @change="handleKnowledgeBaseChange"
            >
              <el-option
                v-for="item in knowledgeBaseOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>

          <div class="ai-card ai-panel__chat">
            <div class="section-title">对话</div>

            <div class="chat-resize-handle" @mousedown="startChatResize">拖动这里调整对话区高度</div>

            <div ref="chatBody" class="chat-body">
              <div
                v-for="(msg, index) in messages"
                :key="index"
                class="chat-item"
                :class="msg.role"
              >
                <div class="chat-item__role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
                <div class="chat-item__content" v-html="renderMessage(msg)"></div>
              </div>
            </div>

            <div class="chat-quick-actions">
              <el-button size="mini" round @click="fillPrompt('帮我说明一下当前页面可以做什么')">
                当前页面能做什么
              </el-button>
              <el-button size="mini" round @click="fillPrompt('给我下一步操作建议')">
                下一步建议
              </el-button>
              <el-button size="mini" round @click="fillPrompt('帮我导航到知识库管理台')">
                导航到知识库
              </el-button>
            </div>

            <el-alert
              v-if="!isLoggedIn"
              class="login-tip"
              type="warning"
              :closable="false"
              title="当前没有识别到完整登录信息，若你已登录，刷新当前页后再试。"
              show-icon
            />

            <el-input
              v-model.trim="input"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder="输入问题，或让 AI 帮你导航到某个业务页面，Ctrl + Enter 可发送"
              @keyup.native.ctrl.enter="send"
            />

            <div class="chat-actions">
              <el-button @click="clearChat">清空</el-button>
              <el-button v-if="sending" @click="stopStream()">停止</el-button>
              <el-button type="primary" :loading="sending" @click="send">
                {{ sending ? '流式生成中...' : '发送' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  aiChatStream,
  aiChatTurn,
  extractErrorMessage,
  getAssistantActiveAiModel,
  getCurrentAiToken,
  getCurrentAiUserId,
  hasAiLoginContext,
  listAssistantAiModels
} from '@/api/aiAssistant'
import { pageKnowledgeBasesByOwner } from '@/api/knowledgeBase'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'
const SELECTED_MODEL_STORAGE_KEY = 'ai_assistant_selected_model_id'
const DRAWER_WIDTH_STORAGE_KEY = 'ai_assistant_drawer_width'
const CHAT_HEIGHT_STORAGE_KEY = 'ai_assistant_chat_height'

export default {
  name: 'AIAssistant',
  data() {
    return {
      visible: false,
      input: '',
      sending: false,
      streamStopper: null,
      sessionId: null,
      selectedKnowledgeBaseId: null,
      knowledgeBaseOptions: [],
      modelOptions: [],
      selectedModelId: null,
      activeModelId: null,
      activeModelName: '',
      drawerWidth: 520,
      chatBodyHeight: 360,
      resizingDrawer: false,
      resizingChat: false,
      resizeStartX: 0,
      resizeStartY: 0,
      resizeStartWidth: 520,
      resizeStartHeight: 360,
      messages: [
        {
          role: 'assistant',
          content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。'
        }
      ]
    }
  },
  computed: {
    userId() {
      return this.resolveUserId()
    },
    isLoggedIn() {
      return this.resolveLoginState()
    },
    currentSceneKnowledgeBase() {
      return this.readCurrentKnowledgeBase()
    },
    displayModelName() {
      const selected = this.modelOptions.find(item => Number(item.id) === Number(this.selectedModelId))
      if (selected && selected.modelName) return selected.modelName
      if (this.activeModelName) return this.activeModelName
      return ''
    },
    sceneMeta() {
      const path = (this.$route && this.$route.path) || ''
      const projectId =
        Number(
          (this.$route && this.$route.query && this.$route.query.projectId) ||
          (this.$route && this.$route.params && this.$route.params.projectId) ||
          (this.currentSceneKnowledgeBase && this.currentSceneKnowledgeBase.projectId) ||
          0
        ) || null

      if (path.includes('/projectdetail')) {
        return {
          bizType: 'PROJECT',
          requestType: 'PROJECT_ASSISTANT',
          sceneCode: 'project.detail',
          label: '当前场景：项目详情',
          projectId
        }
      }

      if (path.includes('/blogwrite')) {
        return {
          bizType: 'BLOG',
          requestType: 'BLOG_ASSISTANT',
          sceneCode: 'blog.write',
          label: '当前场景：博客编辑',
          projectId
        }
      }

      if (path.includes('/knowledge-base')) {
        return {
          bizType: 'GENERAL',
          requestType: 'KNOWLEDGE_QA',
          sceneCode: 'knowledge.base',
          label:
            this.currentSceneKnowledgeBase && this.currentSceneKnowledgeBase.name
              ? `当前场景：知识库管理 / ${this.currentSceneKnowledgeBase.name}`
              : '当前场景：知识库管理',
          projectId
        }
      }

      return {
        bizType: 'GENERAL',
        requestType: 'CHAT',
        sceneCode: 'global.assistant',
        label: '当前场景：通用助手',
        projectId
      }
    },
    sceneLabel() {
      return this.sceneMeta.label
    },
    selectedKnowledgeBaseLabel() {
      if (!this.selectedKnowledgeBaseId) return ''
      const hit = this.knowledgeBaseOptions.find(item => Number(item.id) === Number(this.selectedKnowledgeBaseId))
      if (hit && hit.name) return hit.name
      if (
        this.currentSceneKnowledgeBase &&
        Number(this.currentSceneKnowledgeBase.id) === Number(this.selectedKnowledgeBaseId)
      ) {
        return this.currentSceneKnowledgeBase.name || `知识库 #${this.selectedKnowledgeBaseId}`
      }
      return `知识库 #${this.selectedKnowledgeBaseId}`
    },
    quickNavs() {
      const canOpenKb = this.canOpenKnowledgeBase()
      return [
        { label: '首页', desc: '返回站点首页', path: '/', icon: 'el-icon-house', disabled: false },
        { label: '项目列表', desc: '查看公开项目', path: '/projectlist', icon: 'el-icon-folder-opened', disabled: false },
        { label: '写博客', desc: '进入博客编辑页', path: '/blogwrite', icon: 'el-icon-edit-outline', disabled: false },
        { label: '博客广场', desc: '浏览文章内容', path: '/blog', icon: 'el-icon-document', disabled: false },
        { label: '知识库', desc: canOpenKb ? '打开知识库管理台' : '当前账号暂无权限', path: '/knowledge-base', icon: 'el-icon-notebook-2', disabled: !canOpenKb }
      ]
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initializeAssistant()
      }
    },
    '$route.fullPath'() {
      this.syncSceneKnowledgeBaseSelection()
    },
    selectedKnowledgeBaseId(val) {
      if (typeof window === 'undefined') return
      if (val) {
        localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
      }
    },
    selectedModelId(val) {
      if (typeof window === 'undefined') return
      if (val) {
        localStorage.setItem(SELECTED_MODEL_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_MODEL_STORAGE_KEY)
      }
    }
  },
  mounted() {
    window.addEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.addEventListener('storage', this.handleStorageSync)
    window.addEventListener('mousemove', this.handleGlobalMouseMove)
    window.addEventListener('mouseup', this.stopResize)
    this.restorePanelSize()
    this.syncSceneKnowledgeBaseSelection()
  },
  beforeDestroy() {
    window.removeEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.removeEventListener('storage', this.handleStorageSync)
    window.removeEventListener('mousemove', this.handleGlobalMouseMove)
    window.removeEventListener('mouseup', this.stopResize)
    this.stopResize()
    this.stopStream(true)
  },
  methods: {
    restorePanelSize() {
      if (typeof window === 'undefined') return
      const width = Number(localStorage.getItem(DRAWER_WIDTH_STORAGE_KEY) || 0)
      const chatHeight = Number(localStorage.getItem(CHAT_HEIGHT_STORAGE_KEY) || 0)
      if (width) {
        this.drawerWidth = Math.min(Math.max(width, 420), Math.max(window.innerWidth - 24, 420))
      }
      if (chatHeight) {
        this.chatBodyHeight = Math.min(Math.max(chatHeight, 220), Math.max(window.innerHeight - 320, 220))
      }
    },

    savePanelSize() {
      if (typeof window === 'undefined') return
      localStorage.setItem(DRAWER_WIDTH_STORAGE_KEY, String(this.drawerWidth))
      localStorage.setItem(CHAT_HEIGHT_STORAGE_KEY, String(this.chatBodyHeight))
    },

    startDrawerResize(event) {
      this.resizingDrawer = true
      this.resizeStartX = event.clientX
      this.resizeStartWidth = this.drawerWidth
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'col-resize'
    },

    startChatResize(event) {
      this.resizingChat = true
      this.resizeStartY = event.clientY
      this.resizeStartHeight = this.chatBodyHeight
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'row-resize'
    },

    handleGlobalMouseMove(event) {
      if (this.resizingDrawer) {
        const delta = this.resizeStartX - event.clientX
        const next = this.resizeStartWidth + delta
        const maxWidth = Math.max(window.innerWidth - 24, 420)
        this.drawerWidth = Math.min(Math.max(next, 420), maxWidth)
        return
      }

      if (this.resizingChat) {
        const delta = event.clientY - this.resizeStartY
        const next = this.resizeStartHeight + delta
        const maxHeight = Math.max(window.innerHeight - 320, 220)
        this.chatBodyHeight = Math.min(Math.max(next, 220), maxHeight)
      }
    },

    stopResize() {
      if (this.resizingDrawer || this.resizingChat) {
        this.savePanelSize()
      }
      this.resizingDrawer = false
      this.resizingChat = false
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
    },

    escapeHtml(value) {
      return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
    },

    renderMarkdown(text) {
      const escaped = this.escapeHtml(text || '')
      const lines = escaped.replace(/\r\n/g, '\n').split('\n')
      const html = []
      let inList = false
      let inCode = false
      const closeList = () => {
        if (inList) {
          html.push('</ul>')
          inList = false
        }
      }
      for (const rawLine of lines) {
        const line = rawLine || ''
        if (line.trim().startsWith('```')) {
          closeList()
          if (!inCode) {
            html.push('<pre class="chat-code"><code>')
            inCode = true
          } else {
            html.push('</code></pre>')
            inCode = false
          }
          continue
        }
        if (inCode) {
          html.push(line + '\n')
          continue
        }
        if (/^\s*---+\s*$/.test(line)) {
          closeList()
          html.push('<hr class="chat-hr">')
          continue
        }
        const heading = line.match(/^(#{1,6})\s+(.*)$/)
        if (heading) {
          closeList()
          const level = Math.min(heading[1].length, 6)
          html.push(`<h${level} class="chat-h${level}">${this.renderInlineMarkdown(heading[2])}</h${level}>`)
          continue
        }
        const list = line.match(/^\s*[-*+]\s+(.*)$/)
        if (list) {
          if (!inList) {
            html.push('<ul class="chat-list">')
            inList = true
          }
          html.push(`<li>${this.renderInlineMarkdown(list[1])}</li>`)
          continue
        }
        closeList()
        if (!line.trim()) {
          html.push('<div class="chat-gap"></div>')
          continue
        }
        html.push(`<p>${this.renderInlineMarkdown(line)}</p>`)
      }
      closeList()
      if (inCode) html.push('</code></pre>')
      return html.join('')
    },

    renderInlineMarkdown(text) {
      return String(text || '')
        .replace(/`([^`]+)`/g, '<code class="chat-inline-code">$1</code>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/__([^_]+)__/g, '<strong>$1</strong>')
        .replace(/\*([^*]+)\*/g, '<em>$1</em>')
        .replace(/_([^_]+)_/g, '<em>$1</em>')
        .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
      },

    renderMessage(msg) {
      if (!msg || !msg.content) return ''
      if (msg.role === 'assistant') {
        return this.renderMarkdown(msg.content)
      }
      return this.escapeHtml(msg.content).replace(/\n/g, '<br>')
    },

    resolveLoginState() {
      if (this.resolveUserId()) return true
      try {
        return hasAiLoginContext()
      } catch (e) {
        return false
      }
    },

    resolveUserId() {
      const storeCandidates = [
        this.$store && this.$store.state && this.$store.state.user,
        this.$store && this.$store.state && this.$store.state.user && this.$store.state.user.userInfo,
        this.$store && this.$store.getters && this.$store.getters.userInfo,
        this.$store && this.$store.getters && this.$store.getters.currentUser,
        this.$store && this.$store.getters && this.$store.getters.user,
        this.$store && this.$store.state && this.$store.state.loginUser,
        this.$store && this.$store.state && this.$store.state.account
      ]

      for (const item of storeCandidates) {
        const id = this.pickUserId(item)
        if (id) return id
      }

      try {
        return getCurrentAiUserId()
      } catch (e) {
        return null
      }
    },

    pickUserId(value) {
      if (!value || typeof value !== 'object') return null
      const queue = [value]
      const visited = new Set()
      while (queue.length > 0) {
        const current = queue.shift()
        if (!current || typeof current !== 'object' || visited.has(current)) continue
        visited.add(current)
        const id = Number(
          current.id ||
          current.userId ||
          current.uid ||
          current.user_id ||
          current.memberId ||
          current.accountId ||
          0
        ) || null
        if (id) return id
        ;[
          current.user,
          current.userInfo,
          current.profile,
          current.data,
          current.loginUser,
          current.currentUser,
          current.account,
          current.result
        ].forEach(item => {
          if (item && typeof item === 'object') queue.push(item)
        })
      }
      return null
    },

    handleStorageSync() {
      if (!this.visible) return
      this.syncSceneKnowledgeBaseSelection()
      this.syncSelectedModel()
      this.$forceUpdate()
    },

    extractResponseData(res) {
      if (!res) return null
      const first = Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
      if (
        first &&
        typeof first === 'object' &&
        Object.prototype.hasOwnProperty.call(first, 'data') &&
        (Object.prototype.hasOwnProperty.call(first, 'code') ||
          Object.prototype.hasOwnProperty.call(first, 'msg') ||
          Object.prototype.hasOwnProperty.call(first, 'message') ||
          Object.prototype.hasOwnProperty.call(first, 'success'))
      ) {
        return first.data
      }
      return first
    },

    extractPageData(res) {
      const data = this.extractResponseData(res) || {}
      return {
        content: data.content || data.records || data.list || [],
        total: data.totalElements || data.total || 0
      }
    },

    normalizeKnowledgeBase(raw = {}) {
      return {
        id: raw.id || null,
        name: raw.name || raw.title || '',
        ownerId: raw.ownerId || raw.userId || null,
        projectId: raw.projectId || null,
        scopeType: raw.scopeType || raw.scope || '',
        description: raw.description || '',
        status: raw.status || ''
      }
    },

    normalizeModel(raw = {}) {
      return {
        id: raw.id || null,
        modelName: raw.modelName || raw.name || raw.displayName || raw.providerModel || '',
        modelType: raw.modelType || '',
        providerCode: raw.providerCode || '',
        deploymentMode: raw.deploymentMode || '',
        isActive: raw.isActive === true || Number(raw.id) === Number(this.activeModelId),
        isEnabled: raw.isEnabled !== false
      }
    },

    ensureModelOption(model) {
      if (!model || !model.id) return
      const normalized = this.normalizeModel(model)
      const index = this.modelOptions.findIndex(item => Number(item.id) === Number(normalized.id))
      if (index >= 0) {
        this.$set(this.modelOptions, index, {
          ...this.modelOptions[index],
          ...normalized
        })
        return
      }
      this.modelOptions.push(normalized)
    },

    extractAnswer(data) {
      if (!data) return ''
      return data.answer || data.content || data.message || data.responseText || ''
    },

    readCurrentKnowledgeBase() {
      if (typeof window === 'undefined') return null
      try {
        const raw = localStorage.getItem(CURRENT_KB_STORAGE_KEY)
        if (!raw) return null
        const parsed = JSON.parse(raw)
        return parsed && parsed.id ? parsed : null
      } catch (e) {
        return null
      }
    },

    readSelectedKnowledgeBaseId() {
      if (typeof window === 'undefined') return null
      const raw = localStorage.getItem(SELECTED_KB_STORAGE_KEY)
      return raw ? Number(raw) || null : null
    },

    readSelectedModelId() {
      if (typeof window === 'undefined') return null
      const raw = localStorage.getItem(SELECTED_MODEL_STORAGE_KEY)
      return raw ? Number(raw) || null : null
    },

    ensureKnowledgeBaseOption(kb) {
      if (!kb || !kb.id) return
      const normalized = this.normalizeKnowledgeBase(kb)
      const exists = this.knowledgeBaseOptions.some(item => Number(item.id) === Number(normalized.id))
      if (!exists) {
        this.knowledgeBaseOptions.unshift(normalized)
      }
    },

    syncSceneKnowledgeBaseSelection() {
      const sceneKb = this.readCurrentKnowledgeBase()
      const path = (this.$route && this.$route.path) || ''

      if (sceneKb && sceneKb.id) {
        this.ensureKnowledgeBaseOption(sceneKb)
      }

      if (path.includes('/knowledge-base') && sceneKb && sceneKb.id) {
        this.selectedKnowledgeBaseId = sceneKb.id
        return
      }

      const remembered = this.readSelectedKnowledgeBaseId()
      if (remembered) {
        this.selectedKnowledgeBaseId = remembered
      }
    },

    syncSelectedModel() {
      const remembered = this.readSelectedModelId()
      const hasRemembered = remembered && this.modelOptions.some(item => Number(item.id) === Number(remembered))
      if (hasRemembered) {
        this.selectedModelId = remembered
        return
      }

      const active = this.modelOptions.find(item => Number(item.id) === Number(this.activeModelId))
      if (active) {
        this.selectedModelId = active.id
        return
      }

      const first = this.modelOptions[0]
      this.selectedModelId = first ? first.id : null
    },

    handleSceneKnowledgeBaseChange(event) {
      const kb = event && event.detail ? event.detail : this.readCurrentKnowledgeBase()
      if (kb && kb.id) {
        this.ensureKnowledgeBaseOption(kb)
        if (((this.$route && this.$route.path) || '').includes('/knowledge-base')) {
          this.selectedKnowledgeBaseId = kb.id
        }
      } else if (((this.$route && this.$route.path) || '').includes('/knowledge-base')) {
        this.selectedKnowledgeBaseId = null
      }
    },

    async initializeAssistant() {
      await Promise.all([this.loadKnowledgeBases(), this.loadModels()])
      this.syncSceneKnowledgeBaseSelection()
      this.$nextTick(this.scrollToBottom)
    },

    openDrawer() {
      this.visible = true
    },

    async loadKnowledgeBases() {
      if (!this.userId) {
        this.knowledgeBaseOptions = []
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) {
          this.knowledgeBaseOptions = [this.normalizeKnowledgeBase(sceneKb)]
        }
        return
      }

      try {
        const res = await pageKnowledgeBasesByOwner(this.userId, { page: 0, size: 50 })
        const pageData = this.extractPageData(res)
        this.knowledgeBaseOptions = (pageData.content || []).map(this.normalizeKnowledgeBase)

        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) {
          this.ensureKnowledgeBaseOption(sceneKb)
        }
      } catch (e) {
        this.knowledgeBaseOptions = []
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) {
          this.knowledgeBaseOptions = [this.normalizeKnowledgeBase(sceneKb)]
        }
      }
    },

    async loadModels() {
      let enabledList = []
      let activeModel = null

      try {
        const [enabledRes, activeRes] = await Promise.allSettled([
          listAssistantAiModels(),
          getAssistantActiveAiModel()
        ])

        if (enabledRes.status === 'fulfilled') {
          const payload = this.extractResponseData(enabledRes.value)
          enabledList = Array.isArray(payload) ? payload : []
        }

        if (activeRes.status === 'fulfilled') {
          activeModel = this.extractResponseData(activeRes.value) || null
        }
      } catch (e) {}

      if (activeModel && activeModel.id) {
        this.activeModelId = activeModel.id
        this.activeModelName =
          activeModel.modelName || activeModel.name || activeModel.displayName || activeModel.providerModel || ''
      } else {
        this.activeModelId = null
        this.activeModelName = ''
      }

      this.modelOptions = enabledList.map(this.normalizeModel).filter(item => item.id)
      if (activeModel && activeModel.id) {
        this.ensureModelOption(activeModel)
      }

      this.modelOptions = this.modelOptions
        .slice()
        .sort((a, b) => {
          if ((a.isActive ? 1 : 0) !== (b.isActive ? 1 : 0)) {
            return a.isActive ? -1 : 1
          }
          return Number(a.id || 0) - Number(b.id || 0)
        })

      this.syncSelectedModel()
    },

    handleKnowledgeBaseChange(val) {
      if (typeof window === 'undefined') return
      if (val) {
        localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
      }
    },

    handleModelChange(val) {
      if (typeof window === 'undefined') return
      if (val) {
        localStorage.setItem(SELECTED_MODEL_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_MODEL_STORAGE_KEY)
      }
    },

    go(path, disabled = false) {
      if (disabled) {
        this.$message.warning('当前账号暂无此入口权限')
        return
      }
      this.visible = false
      this.$router.push(path)
    },

    canOpenKnowledgeBase() {
      const checker = this.$hasPermission
      if (typeof checker === 'function') {
        return (
          checker('view:knowledge-base') ||
          checker('view:ai:model-admin') ||
          checker('view:admin:dashboard') ||
          checker('view:permission') ||
          checker('view:menu')
        )
      }
      return true
    },

    fillPrompt(text) {
      this.input = text
    },

    clearChat() {
      this.stopStream(true)
      this.sending = false
      this.sessionId = null
      this.messages = [
        {
          role: 'assistant',
          content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。'
        }
      ]
      this.input = ''
      this.$nextTick(this.scrollToBottom)
    },

    stopStream(silent = false) {
      if (typeof this.streamStopper === 'function') {
        try {
          this.streamStopper()
        } catch (e) {}
      }
      this.streamStopper = null
      if (!silent && this.sending) {
        this.messages.push({
          role: 'assistant',
          content: '已停止本次生成'
        })
      }
      this.sending = false
      this.$nextTick(this.scrollToBottom)
    },

    buildPayload(question) {
      return {
        sessionId: this.sessionId,
        userId: this.userId,
        content: question,
        modelId: this.selectedModelId || this.activeModelId || null,
        requestType: this.sceneMeta.requestType,
        bizType: this.sceneMeta.bizType,
        projectId: this.sceneMeta.projectId || null,
        sceneCode: this.sceneMeta.sceneCode,
        sessionTitle: question.slice(0, 20) || '全局 AI 助手',
        memoryMode: 'SHORT',
        knowledgeBaseIds: this.selectedKnowledgeBaseId ? [this.selectedKnowledgeBaseId] : [],
        defaultKnowledgeBaseId: this.selectedKnowledgeBaseId || null
      }
    },

    async send() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录后再使用 AI 助手')
        return
      }
      if (!this.userId) {
        const token = getCurrentAiToken()
        this.$message.warning(token ? '已检测到 token，但未解析到用户 ID，请刷新当前页后再试' : '请先登录后再使用 AI 助手')
        return
      }
      if (!this.input) {
        this.$message.warning('请输入问题')
        return
      }
      if (this.sending) return

      const question = this.input
      const payload = this.buildPayload(question)
      this.messages.push({ role: 'user', content: question })
      this.input = ''
      this.sending = true

      const assistantMessage = {
        role: 'assistant',
        content: '正在思考...'
      }
      this.messages.push(assistantMessage)
      this.$nextTick(this.scrollToBottom)

      let hasChunk = false
      let finished = false
      let partialText = ''

      const finishStream = () => {
        if (finished) return
        finished = true
        this.streamStopper = null
        this.sending = false
        assistantMessage.content = partialText || '已完成，但没有返回内容'
        this.$nextTick(this.scrollToBottom)
      }

      const handleStreamChunk = chunk => {
        if (!chunk) return

        if (typeof chunk === 'string') {
          if (assistantMessage.content === '正在思考...') {
            assistantMessage.content = ''
          }
          partialText += chunk
          assistantMessage.content = partialText
          hasChunk = true
          this.$nextTick(this.scrollToBottom)
          return
        }

        if (chunk.sessionId) {
          this.sessionId = chunk.sessionId
        }
        if (chunk.modelId) {
          this.activeModelId = chunk.modelId
        }
        if (chunk.modelName) {
          this.activeModelName = chunk.modelName
        }
        if (chunk.delta) {
          if (assistantMessage.content === '正在思考...') {
            assistantMessage.content = ''
          }
          partialText += chunk.delta
          assistantMessage.content = partialText
          hasChunk = true
        }
        if (chunk.finished === true) {
          finishStream()
          return
        }
        this.$nextTick(this.scrollToBottom)
      }

      this.streamStopper = aiChatStream({
        body: payload,
        onMessage: handleStreamChunk,
        onError: async err => {
          this.streamStopper = null
          if (hasChunk) {
            assistantMessage.content = partialText || assistantMessage.content || '生成已中断'
            this.sending = false
            this.$nextTick(this.scrollToBottom)
            return
          }

          try {
            const res = await aiChatTurn(payload)
            const data = this.extractResponseData(res) || {}
            if (data.sessionId) {
              this.sessionId = data.sessionId
            }
            if (data.modelId) {
              this.activeModelId = data.modelId
              this.ensureModelOption({
                id: data.modelId,
                modelName: data.modelName || this.displayModelName || `模型 #${data.modelId}`,
                isActive: true
              })
            }
            if (data.modelName) {
              this.activeModelName = data.modelName
            }
            assistantMessage.content = this.extractAnswer(data) || '已完成，但没有返回内容'
          } catch (fallbackError) {
            assistantMessage.content = `请求失败：${extractErrorMessage(fallbackError)}`
          } finally {
            this.sending = false
            this.$nextTick(this.scrollToBottom)
          }
        },
        onFinish: finishStream
      })
    },

    scrollToBottom() {
      const el = this.$refs.chatBody
      if (el) {
        el.scrollTop = el.scrollHeight
      }
    }
  }
}
</script>

<style scoped>
.global-ai-assistant {
  position: relative;
  z-index: 9999;
}

.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #6a8dff);
  color: #fff;
  box-shadow: 0 14px 32px rgba(64, 158, 255, 0.28);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.ai-fab i {
  font-size: 22px;
  line-height: 1;
}

.ai-fab span {
  margin-top: 4px;
  font-size: 12px;
}

.ai-panel {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fbff 0%, #f5f7fb 100%);
}

.ai-panel__header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 18px 18px 14px;
  border-bottom: 1px solid #edf2f7;
  background: rgba(255, 255, 255, 0.94);
}

.ai-panel__title-wrap {
  min-width: 0;
}

.ai-panel__title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.ai-panel__subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #7b8794;
}

.ai-panel__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 18px 18px;
}

.ai-resize-handle--x {
  position: absolute;
  left: 0;
  top: 0;
  width: 10px;
  height: 100%;
  cursor: col-resize;
  z-index: 2;
}

.ai-panel__scene {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.ai-card {
  margin-bottom: 14px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #edf2f7;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}

.section-title {
  margin-bottom: 10px;
  font-size: 13px;
  color: #475569;
  font-weight: 700;
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.nav-card {
  min-height: 72px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-card:hover {
  border-color: #60a5fa;
  box-shadow: 0 10px 18px rgba(59, 130, 246, 0.12);
  transform: translateY(-1px);
}

.nav-card--disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.nav-card--disabled:hover {
  border-color: #e5e7eb;
  box-shadow: none;
  transform: none;
}

.nav-card__icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-card__text {
  min-width: 0;
}

.nav-card__label {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.nav-card__desc {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.full-width-select {
  width: 100%;
}

.ai-model-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ai-model-option__left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ai-model-option__name {
  color: #1f2937;
}

.ai-model-option__badge {
  padding: 1px 6px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
}

.ai-model-option__meta {
  color: #94a3b8;
  font-size: 12px;
  flex-shrink: 0;
}

.chat-resize-handle {
  margin-bottom: 10px;
  height: 28px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f5f9 100%);
  color: #64748b;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: row-resize;
  user-select: none;
}

.chat-body {
  min-height: 220px;
  height: var(--chat-body-height, 360px);
  max-height: 65vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 12px;
}

.chat-item {
  margin-bottom: 12px;
}

.chat-item__role {
  margin-bottom: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.chat-item__content {
  display: inline-block;
  max-width: 94%;
  padding: 12px 14px;
  border-radius: 16px;
  line-height: 1.8;
  word-break: break-word;
  font-size: 14px;
  text-align: left;
}

.chat-item__content p {
  margin: 0 0 10px;
}

.chat-item__content p:last-child {
  margin-bottom: 0;
}

.chat-item__content strong {
  font-weight: 700;
}

.chat-item__content em {
  font-style: italic;
}

.chat-item__content a {
  color: #2563eb;
  text-decoration: none;
}

.chat-item__content a:hover {
  text-decoration: underline;
}

.chat-item__content .chat-list {
  margin: 0 0 10px 18px;
  padding: 0;
}

.chat-item__content .chat-list li {
  margin: 6px 0;
}

.chat-item__content .chat-gap {
  height: 8px;
}

.chat-item__content .chat-hr {
  border: 0;
  border-top: 1px solid #dbe4f0;
  margin: 12px 0;
}

.chat-item__content .chat-inline-code {
  display: inline-block;
  padding: 1px 6px;
  margin: 0 2px;
  border-radius: 6px;
  background: #e8eef8;
  color: #334155;
  font-size: 13px;
}

.chat-item__content .chat-code {
  margin: 10px 0;
  padding: 12px;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.7;
}

.chat-item__content .chat-h1,
.chat-item__content .chat-h2,
.chat-item__content .chat-h3,
.chat-item__content .chat-h4,
.chat-item__content .chat-h5,
.chat-item__content .chat-h6 {
  margin: 0 0 10px;
  color: #0f172a;
  line-height: 1.5;
}

.chat-item.user {
  text-align: right;
}

.chat-item.user .chat-item__content {
  background: linear-gradient(135deg, #409eff, #5b8cff);
  color: #fff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.2);
}

.chat-item.assistant .chat-item__content {
  background: #f6f8fc;
  color: #1f2937;
  border: 1px solid #e2e8f0;
}

.chat-quick-actions {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.login-tip {
  margin-bottom: 12px;
}

.chat-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.ai-float-enter-active,
.ai-float-leave-active {
  transition: all 0.2s ease;
}

.ai-float-enter,
.ai-float-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.global-ai-assistant ::v-deep .el-drawer__body {
  height: 100%;
  overflow: hidden;
  padding: 0;
}

.global-ai-assistant ::v-deep .ai-drawer {
  min-width: 420px;
  max-width: calc(100vw - 24px);
}

.global-ai-assistant ::v-deep .el-textarea__inner {
  border-radius: 14px;
  min-height: 108px !important;
  resize: vertical;
}

.global-ai-assistant ::v-deep .el-input__inner {
  border-radius: 12px;
}

.global-ai-assistant ::v-deep .el-select-dropdown {
  z-index: 3000 !important;
}

.ai-panel__scroll::-webkit-scrollbar,
.chat-body::-webkit-scrollbar {
  width: 8px;
}

.ai-panel__scroll::-webkit-scrollbar-thumb,
.chat-body::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.45);
  border-radius: 999px;
}

@media (max-width: 768px) {
  .nav-grid {
    grid-template-columns: 1fr;
  }

  .chat-body {
    max-height: 46vh;
  }

  .chat-item__content {
    max-width: 100%;
  }
}
</style>
