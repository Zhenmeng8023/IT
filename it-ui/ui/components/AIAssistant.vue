<template>
  <div class="global-ai-assistant">
    <transition name="ai-float">
      <button v-if="!visible" class="ai-fab" :class="{ 'is-busy': sending }" type="button" @click="openDrawer">
        <i class="el-icon-chat-dot-round"></i>
        <span>{{ sending ? '生成中' : 'AI 助手' }}</span>
      </button>
    </transition>

    <el-drawer
      :visible.sync="visible"
      direction="rtl"
      :size="drawerWidth + 'px'"
      :with-header="false"
      append-to-body
      custom-class="ai-drawer"
    >
      <section class="ai-panel" :style="{ '--chat-body-height': chatBodyHeight + 'px' }">
        <div class="ai-resize-handle ai-resize-handle--x" @mousedown="startDrawerResize"></div>
        <header class="ai-panel__header">
          <div>
            <div class="ai-panel__title">AI 助手</div>
            <div class="ai-panel__subtitle">{{ sceneLabel }}</div>
          </div>
          <div class="ai-panel__header-actions">
            <el-switch v-model="developerMode" active-text="开发" inactive-text="用户" @change="persistDeveloperMode" />
            <el-button type="text" icon="el-icon-close" @click="visible = false" />
          </div>
        </header>

        <div class="ai-panel__context">
          <el-tag size="mini" effect="plain">{{ sceneMeta.sceneCode }}</el-tag>
          <el-tag v-if="displayModelName" size="mini" type="warning" effect="plain">模型：{{ displayModelName }}</el-tag>
          <el-tag v-for="label in selectedKnowledgeBaseLabels" :key="label" size="mini" type="success" effect="plain">
            知识库：{{ label }}
          </el-tag>
          <el-tag v-if="sending" size="mini" type="info" effect="plain">流式生成中</el-tag>
          <el-tag v-if="!isLoggedIn" size="mini" type="danger" effect="plain">未检测到登录态</el-tag>
        </div>

        <main class="ai-workspace">
          <aside class="ai-session-card">
            <div class="section-title section-title--between">
              <span>会话</span>
              <div>
                <el-button type="text" size="mini" icon="el-icon-refresh" @click="loadSessions" />
                <el-button type="text" size="mini" @click="newSession">新建</el-button>
              </div>
            </div>
            <el-alert v-if="areaErrors.session" class="area-alert" type="error" :closable="false" :title="areaErrors.session" show-icon />
            <div v-loading="sessionsLoading || sessionLoading" class="session-list">
              <button
                v-for="item in sessions"
                :key="item.id"
                type="button"
                class="session-item"
                :class="{ 'is-active': Number(item.id) === Number(sessionId) }"
                @click="selectSession(item)"
              >
                <span>{{ item.title || '未命名会话' }}</span>
                <small>{{ formatSessionMeta(item) }}</small>
              </button>
              <div v-if="!sessions.length && !sessionsLoading" class="empty-small">暂无历史会话</div>
            </div>
          </aside>

          <section class="ai-main">
            <div class="ai-card ai-control-card">
              <div class="control-grid">
                <label class="control-field">
                  <span>模型</span>
                  <el-select v-model="selectedModelId" clearable filterable size="small" placeholder="默认模型" @change="handleModelChange">
                    <el-option v-for="item in modelOptions" :key="item.id" :label="item.modelName" :value="item.id">
                      <div class="ai-model-option">
                        <span>{{ item.modelName }}</span>
                        <small>{{ item.isActive ? '当前' : item.providerCode || item.modelType || '' }}</small>
                      </div>
                    </el-option>
                  </el-select>
                </label>
                <label class="control-field">
                  <span>知识库</span>
                  <el-select
                    v-model="selectedKnowledgeBaseIds"
                    multiple
                    collapse-tags
                    clearable
                    filterable
                    size="small"
                    placeholder="选择知识库"
                    @change="handleKnowledgeBaseChange"
                  >
                    <el-option v-for="item in knowledgeBaseOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </label>
              </div>
            </div>

            <div class="ai-card ai-chat-card">
              <div class="section-title section-title--between">
                <span>对话</span>
                <span class="muted-text">{{ sessionId ? `会话 #${sessionId}` : '新会话' }}</span>
              </div>
              <el-alert
                v-if="areaErrors.message"
                class="area-alert"
                :type="areaErrors.messageType || 'error'"
                :closable="false"
                :title="areaErrors.message"
                show-icon
              />
              <div class="chat-resize-handle" @mousedown="startChatResize">拖动调整对话区高度</div>
              <div ref="chatBody" class="chat-body">
                <article v-for="msg in messages" :key="msg.id" class="chat-item" :class="[msg.role, { 'has-error': msg.errorType }]">
                  <div class="chat-item__role">
                    <span>{{ msg.role === 'user' ? '我' : 'AI' }}</span>
                    <small v-if="msg.status">{{ msg.status }}</small>
                  </div>
                  <div class="chat-item__content" v-html="renderMessage(msg)"></div>
                  <div v-if="msg.errorMessage" class="chat-item__error">{{ msg.errorMessage }}</div>
                  <div v-if="msg.role === 'assistant'" class="chat-item__footer">
                    <el-button v-if="msg.sources && msg.sources.length" type="text" size="mini" @click="toggleMessageSources(msg)">
                      {{ msg.sourceOpen ? '收起来源' : `引用来源（${msg.sources.length}）` }}
                    </el-button>
                    <el-button v-if="developerMode && msg.callLogId" type="text" size="mini" @click="openRetrievalDrawerByMessage(msg)">
                      检索日志
                    </el-button>
                  </div>
                  <div v-if="msg.role === 'assistant' && msg.sources && msg.sources.length && msg.sourceOpen" class="chat-item__sources">
                    <div v-for="(source, index) in msg.sources" :key="source.id || `${msg.id}-${index}`" class="source-card">
                      <div class="source-card__top">
                        <strong>{{ source.documentTitle || source.title || '未命中文档标题' }}</strong>
                        <span v-if="source.score !== null && source.score !== undefined">Score {{ source.score }}</span>
                      </div>
                      <div class="source-card__meta">
                        <span v-if="source.knowledgeBaseName">知识库：{{ source.knowledgeBaseName }}</span>
                        <span v-else-if="source.knowledgeBaseId">知识库 #{{ source.knowledgeBaseId }}</span>
                        <span v-if="source.documentId">文档 #{{ source.documentId }}</span>
                        <span v-if="source.chunkId">Chunk #{{ source.chunkId }}</span>
                      </div>
                      <div class="source-card__actions">
                        <el-button type="text" size="mini" @click="locateSourceDocument(source)">定位知识库</el-button>
                        <el-button v-if="developerMode && source.callLogId" type="text" size="mini" @click="openRetrievalDrawer(source.callLogId, source.title || '检索日志')">检索日志</el-button>
                      </div>
                      <p>{{ source.content || source.snippet || '暂无切片内容' }}</p>
                    </div>
                  </div>
                </article>
              </div>
              <div class="chat-quick-actions">
                <el-button size="mini" round @click="fillPrompt('帮我说明一下当前页面可以做什么')">当前页面能做什么</el-button>
                <el-button size="mini" round @click="fillPrompt('给我下一步操作建议')">下一步建议</el-button>
                <el-button size="mini" round @click="fillPrompt('基于当前知识库回答我的问题')">基于知识库回答</el-button>
              </div>
              <el-input v-model.trim="input" type="textarea" :rows="4" resize="vertical" placeholder="输入问题，Ctrl + Enter 发送" @keyup.native.ctrl.enter="send" />
              <div class="chat-actions">
                <el-button @click="clearChat">清空</el-button>
                <el-button :disabled="!lastFailedRequest || sending" @click="retryLast">重试</el-button>
                <el-button v-if="sending" @click="stopStream(false, 'user')">取消</el-button>
                <el-button type="primary" :loading="sending || submitLocked" :disabled="!input.trim()" @click="send">
                  {{ sending ? '流式生成中...' : '发送' }}
                </el-button>
              </div>
            </div>

            <div class="ai-card ai-citation-card">
              <div class="section-title section-title--between">
                <span>来源引用</span>
                <span class="muted-text">{{ visibleCitations.length ? `${visibleCitations.length} 条命中` : '暂无引用' }}</span>
              </div>
              <el-alert v-if="areaErrors.citations" class="area-alert" type="error" :closable="false" :title="areaErrors.citations" show-icon />
              <div v-if="visibleCitations.length" class="citation-list">
                <div v-for="(source, index) in visibleCitations" :key="source.id || index" class="citation-item">
                  <strong>{{ source.documentTitle || source.title || '未命中文档标题' }}</strong>
                  <p>
                    <span v-if="source.knowledgeBaseName">{{ source.knowledgeBaseName }}</span>
                    <span v-else-if="source.knowledgeBaseId">知识库 #{{ source.knowledgeBaseId }}</span>
                    <span v-if="source.documentId"> · 文档 #{{ source.documentId }}</span>
                    <span v-if="source.chunkId"> · Chunk #{{ source.chunkId }}</span>
                  </p>
                  <div>{{ source.content || source.snippet || '暂无切片内容' }}</div>
                </div>
              </div>
              <div v-else class="empty-small">当前回答没有返回引用来源</div>
            </div>

            <div v-if="developerMode" class="ai-card ai-debug-card">
              <div class="section-title section-title--between">
                <span>开发调试</span>
                <el-tag size="mini" type="info" effect="plain">仅开发模式</el-tag>
              </div>
              <el-alert v-if="areaErrors.debug" class="area-alert" type="error" :closable="false" :title="areaErrors.debug" show-icon />
              <div class="debug-row">
                <el-input v-model.trim="debugQuery" size="small" clearable placeholder="输入调试问题；不填时默认使用当前输入框内容" />
                <el-input-number v-model="debugTopK" :min="1" :max="10" size="small" controls-position="right" />
                <el-button size="small" :loading="debugLoading" @click="runKnowledgeBaseDebugSearch">调试检索</el-button>
              </div>
              <pre class="debug-state">{{ debugStateText }}</pre>
            </div>
          </section>
        </main>

        <el-dialog title="检索日志 / 调试结果" :visible.sync="retrievalDrawerVisible" width="980px" append-to-body destroy-on-close>
          <div class="retrieval-meta">{{ currentRetrievalMeta }}</div>
          <el-table :data="retrievalLogs" v-loading="debugLoading" border stripe size="small">
            <el-table-column prop="title" label="命中文档" min-width="200" show-overflow-tooltip />
            <el-table-column prop="knowledgeBaseName" label="知识库" min-width="160" show-overflow-tooltip />
            <el-table-column prop="documentId" label="文档 ID" width="100" />
            <el-table-column prop="chunkId" label="Chunk ID" width="100" />
            <el-table-column prop="score" label="Score" width="100" />
            <el-table-column prop="retrievalMethod" label="检索方式" width="120" />
            <el-table-column label="内容" min-width="320">
              <template slot-scope="{ row }">
                <div class="retrieval-snippet">{{ row.content || row.snippet || '-' }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-dialog>
      </section>
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
import {
  getAiSession,
  listCallRetrievals,
  pageAiSessionMessages,
  pageAiSessions,
  pageKnowledgeBasesByOwner,
  searchKnowledgeBaseDebug
} from '@/api/knowledgeBase'
import {
  classifyAiError,
  extractAiSources,
  extractAnswer,
  extractStreamDeltaText,
  isTerminalStreamChunk,
  normalizeAiSources,
  unwrapApiPayload
} from '@/utils/aiRuntime'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'
const SELECTED_KBS_STORAGE_KEY = 'ai_assistant_selected_kb_ids'
const SELECTED_MODEL_STORAGE_KEY = 'ai_assistant_selected_model_id'
const DRAWER_WIDTH_STORAGE_KEY = 'ai_assistant_drawer_width'
const CHAT_HEIGHT_STORAGE_KEY = 'ai_assistant_chat_height'
const DEV_MODE_STORAGE_KEY = 'ai_assistant_dev_mode'

export default {
  name: 'AIAssistant',
  data() {
    return {
      visible: false,
      input: '',
      sending: false,
      submitLocked: false,
      streamStopper: null,
      activeStream: null,
      streamSeq: 0,
      sessionId: null,
      sessions: [],
      sessionsLoading: false,
      sessionLoading: false,
      selectedKnowledgeBaseIds: [],
      selectedKnowledgeBaseLocked: false,
      knowledgeBaseOptions: [],
      modelOptions: [],
      selectedModelId: null,
      activeModelId: null,
      activeModelName: '',
      developerMode: false,
      drawerWidth: 760,
      chatBodyHeight: 420,
      resizingDrawer: false,
      resizingChat: false,
      resizeStartX: 0,
      resizeStartY: 0,
      resizeStartWidth: 760,
      resizeStartHeight: 420,
      retrievalDrawerVisible: false,
      retrievalLogs: [],
      currentRetrievalMeta: '',
      debugQuery: '',
      debugTopK: 5,
      debugLoading: false,
      areaErrors: {
        message: '',
        messageType: 'error',
        session: '',
        citations: '',
        debug: ''
      },
      lastRequest: null,
      lastFailedRequest: null,
      messages: []
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
    selectedKnowledgeBaseId() {
      const first = Array.isArray(this.selectedKnowledgeBaseIds) ? this.selectedKnowledgeBaseIds[0] : null
      return first ? Number(first) || null : null
    },
    selectedKnowledgeBaseLabels() {
      return (this.selectedKnowledgeBaseIds || []).map(id => {
        const hit = this.knowledgeBaseOptions.find(item => Number(item.id) === Number(id))
        return hit && hit.name ? hit.name : `#${id}`
      })
    },
    displayModelName() {
      const selected = this.modelOptions.find(item => Number(item.id) === Number(this.selectedModelId))
      if (selected && selected.modelName) return selected.modelName
      if (this.activeModelName) return this.activeModelName
      return ''
    },
    visibleCitations() {
      const assistant = this.messages
        .slice()
        .reverse()
        .find(item => item.role === 'assistant' && Array.isArray(item.sources) && item.sources.length)
      return assistant ? assistant.sources : []
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
        return { bizType: 'PROJECT', requestType: 'PROJECT_ASSISTANT', sceneCode: 'project.detail', label: '当前场景：项目详情', projectId }
      }
      if (path.includes('/blogwrite')) {
        return { bizType: 'BLOG', requestType: 'BLOG_ASSISTANT', sceneCode: 'blog.write', label: '当前场景：博客编辑', projectId }
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
      return { bizType: 'GENERAL', requestType: 'CHAT', sceneCode: 'global.assistant', label: '当前场景：通用助手', projectId }
    },
    sceneLabel() {
      return this.sceneMeta.label
    },
    debugStateText() {
      return JSON.stringify({
        sessionId: this.sessionId,
        selectedModelId: this.selectedModelId,
        selectedKnowledgeBaseIds: this.selectedKnowledgeBaseIds,
        activeStreamId: this.activeStream && this.activeStream.id,
        route: this.$route && this.$route.fullPath
      }, null, 2)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initializeAssistant()
      } else {
        this.stopStream(true, 'drawer-close')
      }
    },
    '$route.fullPath'() {
      this.stopStream(true, 'route-change')
      this.syncSceneKnowledgeBaseSelection()
    },
    selectedKnowledgeBaseIds: {
      deep: true,
      handler(val) {
        if (typeof window === 'undefined') return
        const list = Array.isArray(val) ? val.filter(Boolean) : []
        if (list.length) {
          localStorage.setItem(SELECTED_KBS_STORAGE_KEY, JSON.stringify(list))
          localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(list[0]))
        } else {
          localStorage.removeItem(SELECTED_KBS_STORAGE_KEY)
          localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
        }
      }
    },
    selectedModelId(val) {
      if (typeof window === 'undefined') return
      if (val) localStorage.setItem(SELECTED_MODEL_STORAGE_KEY, String(val))
      else localStorage.removeItem(SELECTED_MODEL_STORAGE_KEY)
    }
  },
  created() {
    this.messages = [this.createWelcomeMessage()]
  },
  mounted() {
    window.addEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.addEventListener('ai-assistant-open', this.handleAssistantOpenEvent)
    window.addEventListener('storage', this.handleStorageSync)
    window.addEventListener('mousemove', this.handleGlobalMouseMove)
    window.addEventListener('mouseup', this.stopResize)
    this.restorePanelSize()
    this.restoreDeveloperMode()
    this.syncSceneKnowledgeBaseSelection()
  },
  beforeDestroy() {
    window.removeEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.removeEventListener('ai-assistant-open', this.handleAssistantOpenEvent)
    window.removeEventListener('storage', this.handleStorageSync)
    window.removeEventListener('mousemove', this.handleGlobalMouseMove)
    window.removeEventListener('mouseup', this.stopResize)
    this.stopResize()
    this.stopStream(true, 'destroy')
  },
  methods: {
    createWelcomeMessage() {
      return {
        id: `assistant-welcome-${Date.now()}`,
        role: 'assistant',
        content: '你好，我是 AI 助手。你可以直接提问，也可以从场景入口把预设问题发送到这里。',
        sources: [],
        sourceOpen: false,
        callLogId: null,
        status: ''
      }
    },

    makeMessage(role, content, extra = {}) {
      return {
        id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
        role,
        content: content || '',
        sources: [],
        sourceOpen: false,
        callLogId: null,
        status: '',
        ...extra
      }
    },

    restorePanelSize() {
      if (typeof window === 'undefined') return
      const width = Number(localStorage.getItem(DRAWER_WIDTH_STORAGE_KEY) || 0)
      const chatHeight = Number(localStorage.getItem(CHAT_HEIGHT_STORAGE_KEY) || 0)
      if (width) this.drawerWidth = Math.min(Math.max(width, 560), Math.max(window.innerWidth - 24, 560))
      if (chatHeight) this.chatBodyHeight = Math.min(Math.max(chatHeight, 260), Math.max(window.innerHeight - 320, 260))
    },

    savePanelSize() {
      if (typeof window === 'undefined') return
      localStorage.setItem(DRAWER_WIDTH_STORAGE_KEY, String(this.drawerWidth))
      localStorage.setItem(CHAT_HEIGHT_STORAGE_KEY, String(this.chatBodyHeight))
    },

    restoreDeveloperMode() {
      if (typeof window === 'undefined') return
      this.developerMode = localStorage.getItem(DEV_MODE_STORAGE_KEY) === '1'
    },

    persistDeveloperMode() {
      if (typeof window === 'undefined') return
      localStorage.setItem(DEV_MODE_STORAGE_KEY, this.developerMode ? '1' : '0')
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
        this.drawerWidth = Math.min(Math.max(this.resizeStartWidth + delta, 560), Math.max(window.innerWidth - 24, 560))
        return
      }
      if (this.resizingChat) {
        const delta = event.clientY - this.resizeStartY
        this.chatBodyHeight = Math.min(Math.max(this.resizeStartHeight + delta, 260), Math.max(window.innerHeight - 320, 260))
      }
    },

    stopResize() {
      if (this.resizingDrawer || this.resizingChat) this.savePanelSize()
      this.resizingDrawer = false
      this.resizingChat = false
      if (typeof document !== 'undefined') {
        document.body.style.userSelect = ''
        document.body.style.cursor = ''
      }
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
      lines.forEach(rawLine => {
        const line = rawLine || ''
        if (line.trim().startsWith('```')) {
          closeList()
          html.push(inCode ? '</code></pre>' : '<pre class="chat-code"><code>')
          inCode = !inCode
          return
        }
        if (inCode) {
          html.push(`${line}\n`)
          return
        }
        const heading = line.match(/^(#{1,6})\s+(.*)$/)
        if (heading) {
          closeList()
          const level = Math.min(heading[1].length, 6)
          html.push(`<h${level} class="chat-h${level}">${this.renderInlineMarkdown(heading[2])}</h${level}>`)
          return
        }
        const list = line.match(/^\s*[-*+]\s+(.*)$/)
        if (list) {
          if (!inList) {
            html.push('<ul class="chat-list">')
            inList = true
          }
          html.push(`<li>${this.renderInlineMarkdown(list[1])}</li>`)
          return
        }
        closeList()
        html.push(line.trim() ? `<p>${this.renderInlineMarkdown(line)}</p>` : '<div class="chat-gap"></div>')
      })
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
      return msg.role === 'assistant' ? this.renderMarkdown(msg.content) : this.escapeHtml(msg.content).replace(/\n/g, '<br>')
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
      try {
        return getCurrentAiUserId()
      } catch (e) {
        return null
      }
    },

    extractResponseData(res) {
      return unwrapApiPayload(res)
    },

    extractPageData(res) {
      const data = this.extractResponseData(res) || {}
      return { content: data.content || data.records || data.list || [], total: data.totalElements || data.total || 0 }
    },

    normalizeKnowledgeBase(raw = {}) {
      return {
        id: raw.id || raw.knowledgeBaseId || null,
        name: raw.name || raw.knowledgeBaseName || raw.title || '',
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

    normalizeSession(raw = {}) {
      const boundKbs = Array.isArray(raw.boundKnowledgeBases) ? raw.boundKnowledgeBases.map(this.normalizeKnowledgeBase) : []
      const boundIds = Array.isArray(raw.boundKnowledgeBaseIds) ? raw.boundKnowledgeBaseIds : boundKbs.map(item => item.id).filter(Boolean)
      return {
        id: raw.id || raw.sessionId || null,
        title: raw.title || raw.sessionTitle || raw.name || '',
        updatedAt: raw.updatedAt || raw.lastMessageAt || raw.createTime || raw.createdAt || '',
        boundKnowledgeBaseIds: boundIds.filter(Boolean),
        boundKnowledgeBases: boundKbs,
        defaultKnowledgeBaseId: raw.defaultKnowledgeBaseId || null,
        recentKnowledgeBaseId: raw.recentKnowledgeBaseId || null,
        modelId: raw.modelId || raw.defaultModelId || null
      }
    },

    normalizeMessage(raw = {}) {
      const roleValue = String(raw.role || raw.messageRole || raw.senderType || raw.type || '').toLowerCase()
      const role = roleValue.includes('user') || roleValue === 'human' ? 'user' : 'assistant'
      const callLogId = raw.callLogId || raw.aiCallLogId || null
      return this.makeMessage(role, raw.content || raw.message || raw.answer || raw.text || '', {
        id: raw.id || raw.messageId || `${role}-${Date.now()}-${Math.random()}`,
        sources: extractAiSources(raw, { callLogId }),
        sourceOpen: false,
        callLogId,
        modelName: raw.modelName || ''
      })
    },

    ensureModelOption(model) {
      if (!model || !model.id) return
      const normalized = this.normalizeModel(model)
      const index = this.modelOptions.findIndex(item => Number(item.id) === Number(normalized.id))
      if (index >= 0) {
        this.$set(this.modelOptions, index, { ...this.modelOptions[index], ...normalized })
      } else {
        this.modelOptions.push(normalized)
      }
    },

    ensureKnowledgeBaseOption(kb) {
      if (!kb || !kb.id) return
      const normalized = this.normalizeKnowledgeBase(kb)
      const exists = this.knowledgeBaseOptions.some(item => Number(item.id) === Number(normalized.id))
      if (!exists) this.knowledgeBaseOptions.unshift(normalized)
    },

    setAreaError(area, error, fallback, type = 'error') {
      const aiError = typeof error === 'string' ? null : classifyAiError(error, fallback)
      const message = typeof error === 'string' ? error : aiError ? `${aiError.label}：${aiError.message}` : fallback
      this.$set(this.areaErrors, area, message || '')
      if (area === 'message') this.$set(this.areaErrors, 'messageType', type)
      return aiError
    },

    clearAreaError(area) {
      this.$set(this.areaErrors, area, '')
      if (area === 'message') this.$set(this.areaErrors, 'messageType', 'error')
    },

    handleStorageSync() {
      if (!this.visible) return
      this.syncSceneKnowledgeBaseSelection()
      this.syncSelectedModel()
      this.$forceUpdate()
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

    readSelectedKnowledgeBaseIds() {
      if (typeof window === 'undefined') return []
      try {
        const rawList = localStorage.getItem(SELECTED_KBS_STORAGE_KEY)
        if (rawList) {
          const parsed = JSON.parse(rawList)
          if (Array.isArray(parsed)) return parsed.map(item => Number(item) || null).filter(Boolean)
        }
      } catch (e) {}
      const raw = localStorage.getItem(SELECTED_KB_STORAGE_KEY)
      const id = raw ? Number(raw) || null : null
      return id ? [id] : []
    },

    readSelectedModelId() {
      if (typeof window === 'undefined') return null
      const raw = localStorage.getItem(SELECTED_MODEL_STORAGE_KEY)
      return raw ? Number(raw) || null : null
    },

    setKnowledgeBaseSelection(ids, options = {}) {
      const { resetSession = false, manual = false, source = '' } = options
      const list = (Array.isArray(ids) ? ids : (ids ? [ids] : [])).map(item => Number(item) || null).filter(Boolean)
      const current = (this.selectedKnowledgeBaseIds || []).map(item => Number(item) || null).filter(Boolean)
      const changed = list.join(',') !== current.join(',')
      this.selectedKnowledgeBaseIds = list
      this.selectedKnowledgeBaseLocked = !!(manual && list.length)
      if (resetSession && changed) {
        this.stopStream(true, `kb-change:${source || 'unknown'}`)
        this.sessionId = null
        this.messages = [
          this.createWelcomeMessage(),
          this.makeMessage('assistant', '已切换知识库，本次对话将使用新的检索上下文。', { status: '上下文已更新' })
        ]
        this.$nextTick(this.scrollToBottom)
      }
    },

    syncSceneKnowledgeBaseSelection() {
      const sceneKb = this.readCurrentKnowledgeBase()
      const path = (this.$route && this.$route.path) || ''
      if (sceneKb && sceneKb.id) this.ensureKnowledgeBaseOption(sceneKb)
      if (path.includes('/knowledge-base') && sceneKb && sceneKb.id && !this.selectedKnowledgeBaseLocked) {
        this.setKnowledgeBaseSelection([sceneKb.id], { manual: false, source: 'scene-sync' })
        return
      }
      if (!this.selectedKnowledgeBaseIds.length) {
        this.setKnowledgeBaseSelection(this.readSelectedKnowledgeBaseIds(), { manual: true, source: 'remembered' })
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
      this.selectedModelId = active ? active.id : (this.modelOptions[0] ? this.modelOptions[0].id : null)
    },

    handleSceneKnowledgeBaseChange(event) {
      const kb = event && event.detail ? event.detail : this.readCurrentKnowledgeBase()
      const path = (this.$route && this.$route.path) || ''
      if (!path.includes('/knowledge-base')) return
      if (kb && kb.id) {
        this.ensureKnowledgeBaseOption(kb)
        if (!this.selectedKnowledgeBaseLocked) this.setKnowledgeBaseSelection([kb.id], { manual: false, source: 'scene-event' })
      } else if (!this.selectedKnowledgeBaseLocked) {
        this.setKnowledgeBaseSelection([], { manual: false, source: 'scene-clear' })
      }
    },

    async initializeAssistant() {
      await Promise.all([this.loadKnowledgeBases(), this.loadModels(), this.loadSessions()])
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
        if (sceneKb && sceneKb.id) this.knowledgeBaseOptions = [this.normalizeKnowledgeBase(sceneKb)]
        return
      }
      try {
        const res = await pageKnowledgeBasesByOwner(this.userId, { page: 0, size: 80 })
        const pageData = this.extractPageData(res)
        this.knowledgeBaseOptions = (pageData.content || []).map(this.normalizeKnowledgeBase)
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) this.ensureKnowledgeBaseOption(sceneKb)
        this.clearAreaError('session')
      } catch (e) {
        this.knowledgeBaseOptions = []
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) this.knowledgeBaseOptions = [this.normalizeKnowledgeBase(sceneKb)]
        this.setAreaError('session', e, '加载知识库失败')
      }
    },

    async loadModels() {
      let enabledList = []
      let activeModel = null
      let modelLoadFailed = false
      try {
        const [enabledRes, activeRes] = await Promise.allSettled([listAssistantAiModels(), getAssistantActiveAiModel()])
        if (enabledRes.status === 'fulfilled') {
          const payload = this.extractResponseData(enabledRes.value)
          enabledList = Array.isArray(payload) ? payload : []
        } else {
          modelLoadFailed = true
        }
        if (activeRes.status === 'fulfilled') {
          activeModel = this.extractResponseData(activeRes.value) || null
        } else {
          modelLoadFailed = true
        }
      } catch (e) {
        modelLoadFailed = true
      }
      if (activeModel && activeModel.id) {
        this.activeModelId = activeModel.id
        this.activeModelName = activeModel.modelName || activeModel.name || activeModel.displayName || activeModel.providerModel || ''
      } else {
        this.activeModelId = null
        this.activeModelName = ''
      }
      this.modelOptions = enabledList.map(this.normalizeModel).filter(item => item.id)
      if (activeModel && activeModel.id) this.ensureModelOption(activeModel)
      this.modelOptions = this.modelOptions.slice().sort((a, b) => {
        if ((a.isActive ? 1 : 0) !== (b.isActive ? 1 : 0)) return a.isActive ? -1 : 1
        return Number(a.id || 0) - Number(b.id || 0)
      })
      this.syncSelectedModel()
      if (modelLoadFailed && !this.modelOptions.length) {
        this.setAreaError('message', '加载模型列表失败，将尝试使用后端默认模型。', '', 'warning')
      }
    },

    async loadSessions() {
      if (!this.isLoggedIn) {
        this.sessions = []
        return
      }
      this.sessionsLoading = true
      this.clearAreaError('session')
      try {
        const res = await pageAiSessions({ page: 0, size: 30, userId: this.userId || undefined })
        const pageData = this.extractPageData(res)
        this.sessions = (pageData.content || []).map(this.normalizeSession).filter(item => item.id)
        this.sessions.forEach(item => (item.boundKnowledgeBases || []).forEach(this.ensureKnowledgeBaseOption))
      } catch (e) {
        this.setAreaError('session', e, '加载会话列表失败')
      } finally {
        this.sessionsLoading = false
      }
    },

    async selectSession(item) {
      if (!item || !item.id || Number(item.id) === Number(this.sessionId)) return
      this.stopStream(true, 'switch-session')
      this.sessionLoading = true
      this.clearAreaError('session')
      try {
        let session = item
        try {
          const detailRes = await getAiSession(item.id)
          session = this.normalizeSession(this.extractResponseData(detailRes) || item)
        } catch (e) {
          session = item
        }
        this.sessionId = session.id
        this.applySessionKnowledgeContext(session)
        const msgRes = await pageAiSessionMessages(session.id, { page: 0, size: 80 })
        const pageData = this.extractPageData(msgRes)
        const list = (pageData.content || []).map(this.normalizeMessage)
        this.messages = list.length ? list : [this.createWelcomeMessage()]
        this.$nextTick(this.scrollToBottom)
      } catch (e) {
        this.setAreaError('session', e, '切换会话失败')
      } finally {
        this.sessionLoading = false
      }
    },

    applySessionKnowledgeContext(session) {
      const boundBases = Array.isArray(session.boundKnowledgeBases) ? session.boundKnowledgeBases : []
      boundBases.forEach(this.ensureKnowledgeBaseOption)
      const ids = []
      if (session.defaultKnowledgeBaseId) ids.push(session.defaultKnowledgeBaseId)
      if (session.recentKnowledgeBaseId && !ids.includes(session.recentKnowledgeBaseId)) ids.push(session.recentKnowledgeBaseId)
      ;(session.boundKnowledgeBaseIds || []).forEach(id => {
        if (id && !ids.includes(id)) ids.push(id)
      })
      if (ids.length) this.setKnowledgeBaseSelection(ids, { manual: false, source: 'session' })
      if (session.modelId) this.selectedModelId = session.modelId
    },

    newSession() {
      this.stopStream(true, 'new-session')
      this.sessionId = null
      this.lastFailedRequest = null
      this.messages = [this.createWelcomeMessage()]
      this.clearAreaError('message')
      this.clearAreaError('citations')
      this.$nextTick(this.scrollToBottom)
    },

    formatSessionMeta(item) {
      const parts = []
      if (item.boundKnowledgeBaseIds && item.boundKnowledgeBaseIds.length) parts.push(`${item.boundKnowledgeBaseIds.length} 个知识库`)
      if (item.updatedAt) parts.push(String(item.updatedAt).slice(0, 16).replace('T', ' '))
      return parts.join(' · ') || '最近会话'
    },

    handleKnowledgeBaseChange(val) {
      this.setKnowledgeBaseSelection(val, { resetSession: true, manual: true, source: 'user-select' })
    },

    handleModelChange(val) {
      if (this.sending) this.stopStream(false, 'model-change')
      if (typeof window === 'undefined') return
      if (val) localStorage.setItem(SELECTED_MODEL_STORAGE_KEY, String(val))
      else localStorage.removeItem(SELECTED_MODEL_STORAGE_KEY)
    },

    fillPrompt(text) {
      this.input = text
    },

    handleAssistantOpenEvent(event) {
      const detail = (event && event.detail) || {}
      this.visible = true
      if (Array.isArray(detail.knowledgeBaseIds) && detail.knowledgeBaseIds.length) {
        this.setKnowledgeBaseSelection(detail.knowledgeBaseIds, { manual: true, source: 'dock' })
      }
      if (detail.sessionId) this.sessionId = detail.sessionId
      if (detail.prompt) {
        this.input = detail.prompt
        if (detail.autoSend !== false) this.$nextTick(() => this.send())
      }
    },

    clearChat() {
      this.stopStream(true, 'clear')
      this.sessionId = null
      this.lastFailedRequest = null
      this.messages = [this.createWelcomeMessage()]
      this.input = ''
      this.clearAreaError('message')
      this.clearAreaError('citations')
      this.$nextTick(this.scrollToBottom)
    },

    isActiveStream(streamId) {
      return this.activeStream && this.activeStream.id === streamId
    },

    stopStream(silent = false, reason = 'user') {
      const active = this.activeStream
      if (typeof this.streamStopper === 'function') {
        try {
          this.streamStopper()
        } catch (e) {}
      }
      this.streamStopper = null
      this.activeStream = null
      this.sending = false
      this.submitLocked = false
      if (!silent && active && active.assistantMessage) {
        const message = active.assistantMessage
        const hasText = message.content && message.content !== '正在思考...'
        message.status = '已取消'
        message.errorType = 'canceled'
        message.errorMessage = '本次请求已取消'
        message.content = hasText ? message.content : '已取消本次请求'
        this.setAreaError('message', reason === 'user' ? '已取消本次请求' : '上下文变化，已取消正在生成的回答', '', 'warning')
      }
      this.$nextTick(this.scrollToBottom)
    },

    buildPayload(question, lockedKnowledgeBaseIds = null) {
      const kbIds = (Array.isArray(lockedKnowledgeBaseIds) ? lockedKnowledgeBaseIds : this.selectedKnowledgeBaseIds)
        .map(item => Number(item) || null)
        .filter(Boolean)
      return {
        sessionId: this.sessionId,
        userId: this.userId || undefined,
        content: question,
        modelId: this.selectedModelId || this.activeModelId || null,
        requestType: this.sceneMeta.requestType,
        bizType: this.sceneMeta.bizType,
        projectId: this.sceneMeta.projectId || null,
        sceneCode: this.sceneMeta.sceneCode,
        sessionTitle: question.slice(0, 28) || 'AI 助手会话',
        memoryMode: 'SHORT',
        knowledgeBaseIds: kbIds,
        defaultKnowledgeBaseId: kbIds[0] || null
      }
    },

    async send() {
      if (!this.isLoggedIn) {
        const token = getCurrentAiToken()
        this.setAreaError('message', token ? '已检测到 token，但登录态未完成，请刷新当前页后再试' : '请先登录后再使用 AI 助手', '', 'warning')
        return
      }
      const question = (this.input || '').trim()
      if (!question) {
        this.setAreaError('message', '请输入问题', '', 'warning')
        return
      }
      if (this.sending || this.submitLocked) return
      this.clearAreaError('message')
      this.clearAreaError('citations')
      this.submitLocked = true
      const lockedKnowledgeBaseIds = this.selectedKnowledgeBaseIds.slice()
      const payload = this.buildPayload(question, lockedKnowledgeBaseIds)
      this.lastRequest = { question, payload }
      this.lastFailedRequest = null
      this.messages.push(this.makeMessage('user', question))
      this.input = ''
      const assistantMessage = this.makeMessage('assistant', '正在思考...', { status: '流式生成中', modelName: this.displayModelName })
      this.messages.push(assistantMessage)
      this.$nextTick(this.scrollToBottom)

      let partialText = ''
      let hasChunk = false
      let completed = false
      const streamId = `stream-${Date.now()}-${++this.streamSeq}`
      this.sending = true
      this.activeStream = { id: streamId, assistantMessage, payload, question, knowledgeBaseIds: lockedKnowledgeBaseIds }

      const finishStream = async () => {
        if (!this.isActiveStream(streamId) || completed) return
        completed = true
        this.streamStopper = null
        this.activeStream = null
        this.sending = false
        this.submitLocked = false
        assistantMessage.status = '已完成'
        assistantMessage.content = partialText || assistantMessage.content || '已完成，但没有返回内容'
        await this.ensureAssistantMessageSourceLoaded(assistantMessage)
        this.loadSessions()
        this.$nextTick(this.scrollToBottom)
      }

      const handleChunk = chunk => {
        if (!this.isActiveStream(streamId) || !chunk) return
        if (typeof chunk === 'object') {
          if (chunk.sessionId) this.sessionId = chunk.sessionId
          if (chunk.modelId) this.activeModelId = chunk.modelId
          if (chunk.modelName) {
            this.activeModelName = chunk.modelName
            assistantMessage.modelName = chunk.modelName
          }
          if (chunk.callLogId) assistantMessage.callLogId = chunk.callLogId
          const sources = extractAiSources(chunk, { callLogId: chunk.callLogId || assistantMessage.callLogId, knowledgeBaseId: lockedKnowledgeBaseIds[0] || null })
          if (sources.length) {
            assistantMessage.sources = sources
            assistantMessage.sourceOpen = true
          }
        }
        const delta = chunk && typeof chunk === 'object' ? (chunk.delta || chunk.contentDelta || chunk.answerDelta || '') : extractStreamDeltaText(chunk)
        if (delta) {
          if (assistantMessage.content === '正在思考...') assistantMessage.content = ''
          partialText += delta
          assistantMessage.content = partialText
          hasChunk = true
        } else {
          const fullText = typeof chunk === 'object' ? extractAnswer(chunk) : extractStreamDeltaText(chunk)
          if (fullText && (!partialText || fullText.length >= partialText.length)) {
            partialText = fullText
            assistantMessage.content = partialText
            hasChunk = true
          }
        }
        if (isTerminalStreamChunk(chunk)) {
          finishStream()
          return
        }
        this.$nextTick(this.scrollToBottom)
      }

      const fallbackTurn = async err => {
        const aiError = classifyAiError(err)
        if (['unauthorized', 'forbidden', 'canceled'].includes(aiError.type)) throw err
        const res = await aiChatTurn(payload)
        const data = this.extractResponseData(res) || {}
        if (data.sessionId) this.sessionId = data.sessionId
        if (data.modelId) {
          this.activeModelId = data.modelId
          this.ensureModelOption({ id: data.modelId, modelName: data.modelName || this.displayModelName || `模型 #${data.modelId}`, isActive: true })
        }
        if (data.modelName) {
          this.activeModelName = data.modelName
          assistantMessage.modelName = data.modelName
        }
        if (data.callLogId) assistantMessage.callLogId = data.callLogId
        const sources = extractAiSources(data, { callLogId: data.callLogId || assistantMessage.callLogId })
        if (sources.length) {
          assistantMessage.sources = sources
          assistantMessage.sourceOpen = true
        }
        partialText = extractAnswer(data) || '已完成，但没有返回内容'
        assistantMessage.content = partialText
        assistantMessage.status = '已完成'
      }

      this.streamStopper = aiChatStream({
        body: payload,
        timeout: 300000,
        onMessage: handleChunk,
        onError: async err => {
          if (!this.isActiveStream(streamId)) return
          this.streamStopper = null
          this.activeStream = null
          this.sending = false
          this.submitLocked = false
          if (hasChunk) {
            const aiError = classifyAiError(err, '流式响应中断')
            assistantMessage.content = partialText || assistantMessage.content || '生成已中断'
            assistantMessage.status = '已中断'
            assistantMessage.errorType = 'stream_interrupted'
            assistantMessage.errorMessage = `${aiError.label}：流式响应中断，可重试本次问题`
            this.lastFailedRequest = { question, payload }
            this.setAreaError('message', '流式响应中断，已保留当前已生成内容，可点击重试。')
            this.$nextTick(this.scrollToBottom)
            return
          }
          try {
            await fallbackTurn(err)
            await finishStream()
          } catch (fallbackError) {
            const aiError = classifyAiError(fallbackError, extractErrorMessage(fallbackError, '请求失败'))
            assistantMessage.content = '请求失败'
            assistantMessage.status = aiError.label
            assistantMessage.errorType = aiError.type
            assistantMessage.errorMessage = aiError.message
            this.lastFailedRequest = { question, payload }
            this.setAreaError('message', fallbackError, 'AI 请求失败')
            this.$nextTick(this.scrollToBottom)
          }
        },
        onFinish: finishStream
      })
    },

    retryLast() {
      if (!this.lastFailedRequest || this.sending) return
      this.input = this.lastFailedRequest.question
      this.$nextTick(() => this.send())
    },

    async ensureAssistantMessageSourceLoaded(message) {
      if (!message || !message.callLogId || (Array.isArray(message.sources) && message.sources.length)) return
      try {
        const res = await listCallRetrievals(message.callLogId)
        const data = this.extractResponseData(res)
        const list = Array.isArray(data) ? data : (data && (data.content || data.list || data.records)) || []
        this.$set(message, 'sources', normalizeAiSources(list, { callLogId: message.callLogId }))
        this.clearAreaError('citations')
      } catch (e) {
        this.setAreaError('citations', e, '加载引用来源失败')
      }
    },

    toggleMessageSources(message) {
      if (!message) return
      const next = !message.sourceOpen
      this.$set(message, 'sourceOpen', next)
      if (next) this.ensureAssistantMessageSourceLoaded(message)
    },

    locateSourceDocument(source) {
      if (!source || !source.documentId) {
        this.$message.info('当前来源没有文档 ID')
        return
      }
      const kbId = source.knowledgeBaseId || this.selectedKnowledgeBaseId || ''
      this.visible = false
      this.$router.push({ path: '/knowledge-base', query: { kbId, documentId: source.documentId } })
    },

    async openRetrievalDrawerByMessage(message) {
      if (!message || !message.callLogId) {
        this.$message.warning('当前消息没有关联检索日志')
        return
      }
      await this.openRetrievalDrawer(message.callLogId, message.modelName || '检索日志')
    },

    async openRetrievalDrawer(callLogId, title = '检索日志') {
      if (!callLogId) return
      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = title
      this.debugLoading = true
      this.clearAreaError('debug')
      try {
        const res = await listCallRetrievals(callLogId)
        const data = this.extractResponseData(res)
        const list = Array.isArray(data) ? data : (data && (data.content || data.list || data.records)) || []
        this.retrievalLogs = normalizeAiSources(list, { callLogId })
      } catch (e) {
        this.setAreaError('debug', e, '加载检索日志失败')
      } finally {
        this.debugLoading = false
      }
    },

    async runKnowledgeBaseDebugSearch() {
      if (!this.developerMode) return
      if (!this.selectedKnowledgeBaseId) {
        this.setAreaError('debug', '请先选择知识库')
        return
      }
      const query = (this.debugQuery || this.input || '').trim()
      if (!query) {
        this.setAreaError('debug', '请输入调试问题')
        return
      }
      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `检索调试：${query}`
      this.debugLoading = true
      this.clearAreaError('debug')
      try {
        const res = await searchKnowledgeBaseDebug(this.selectedKnowledgeBaseId, { query, topK: this.debugTopK || 5 })
        const payload = this.extractResponseData(res) || {}
        const hits = Array.isArray(payload.hits) ? payload.hits : extractAiSources(payload)
        this.retrievalLogs = normalizeAiSources(hits, { knowledgeBaseId: this.selectedKnowledgeBaseId })
      } catch (e) {
        this.setAreaError('debug', e, '检索调试失败')
      } finally {
        this.debugLoading = false
      }
    },

    scrollToBottom() {
      const el = this.$refs.chatBody
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
.global-ai-assistant {
  position: relative;
  z-index: 1100;
}

.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 66px;
  height: 66px;
  border: 0;
  border-radius: 8px;
  background: var(--it-accent);
  color: #fff;
  box-shadow: var(--it-shadow-strong, var(--it-shadow));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  z-index: 1100;
}

.ai-fab.is-busy {
  background: #67c23a;
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
  background: var(--it-page-bg);
  color: var(--it-text);
}

.ai-panel__header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid var(--it-border);
  background: var(--it-overlay);
}

.ai-panel__header-actions,
.source-card__top,
.source-card__meta,
.source-card__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-panel__title {
  font-size: 20px;
  font-weight: 700;
  color: var(--it-text);
}

.ai-panel__subtitle,
.muted-text,
.empty-small,
.session-item small,
.source-card__meta,
.citation-item p {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.ai-panel__subtitle {
  margin-top: 6px;
}

.ai-panel__context {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.ai-workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 18px 18px;
  overflow: hidden;
}

.ai-main {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.ai-session-card,
.ai-card {
  border: 1px solid var(--it-border);
  border-radius: 8px;
  background: var(--it-overlay);
  box-shadow: var(--it-shadow);
}

.ai-session-card {
  min-height: 0;
  overflow: hidden;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.ai-card {
  margin-bottom: 14px;
  padding: 14px;
}

.section-title {
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--it-text-muted);
  font-weight: 700;
}

.section-title--between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.area-alert {
  margin-bottom: 10px;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  gap: 8px;
  align-content: start;
}

.session-item {
  width: 100%;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  background: var(--it-surface-solid);
  color: var(--it-text);
  padding: 10px;
  text-align: left;
  cursor: pointer;
}

.session-item.is-active {
  border-color: var(--it-accent);
  background: var(--it-accent-soft);
}

.session-item span {
  display: block;
  font-weight: 700;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item small {
  display: block;
  margin-top: 4px;
}

.control-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 12px;
}

.control-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.control-field > span {
  color: var(--it-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.ai-model-option {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.chat-resize-handle {
  margin-bottom: 10px;
  height: 28px;
  border: 1px dashed var(--it-border-strong);
  border-radius: 8px;
  background: var(--it-surface-muted);
  color: var(--it-text-muted);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: row-resize;
  user-select: none;
}

.chat-body {
  min-height: 260px;
  height: var(--chat-body-height, 420px);
  max-height: 65vh;
  overflow-y: auto;
  background: var(--it-surface-solid);
  border: 1px solid var(--it-border);
  border-radius: 8px;
  padding: 12px;
}

.chat-item {
  margin-bottom: 12px;
}

.chat-item__role {
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.chat-item.user .chat-item__role {
  justify-content: flex-end;
}

.chat-item__content {
  display: inline-block;
  max-width: 94%;
  padding: 12px 14px;
  border-radius: 8px;
  line-height: 1.8;
  word-break: break-word;
  font-size: 14px;
  text-align: left;
}

.chat-item__content p {
  margin: 0 0 10px;
}

.chat-item__content p:last-child,
.source-card p,
.citation-item p {
  margin-bottom: 0;
}

.chat-item__content .chat-list {
  margin: 0 0 10px 18px;
  padding: 0;
}

.chat-item__content .chat-gap {
  height: 8px;
}

.chat-item__content .chat-inline-code {
  display: inline-block;
  padding: 1px 6px;
  margin: 0 2px;
  border-radius: 6px;
  background: var(--it-surface-muted);
  color: var(--it-text-muted);
  font-size: 13px;
}

.chat-item__content .chat-code {
  margin: 10px 0;
  padding: 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.7;
}

.chat-item.user {
  text-align: right;
}

.chat-item.user .chat-item__content {
  background: var(--it-accent);
  color: #fff;
}

.chat-item.assistant .chat-item__content {
  background: var(--it-surface-muted);
  color: var(--it-text);
  border: 1px solid var(--it-border);
}

.chat-item.has-error .chat-item__content {
  border-color: #f56c6c;
}

.chat-item__error {
  margin-top: 6px;
  color: #f56c6c;
  font-size: 12px;
}

.chat-item__footer,
.chat-quick-actions,
.chat-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-item__footer,
.chat-actions {
  justify-content: flex-end;
  margin-top: 8px;
}

.chat-quick-actions {
  margin: 12px 0;
}

.chat-item__sources,
.citation-list {
  margin-top: 10px;
  display: grid;
  gap: 10px;
}

.source-card,
.citation-item {
  border: 1px solid var(--it-border);
  background: var(--it-surface-muted);
  border-radius: 8px;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.source-card__top {
  justify-content: space-between;
}

.source-card__meta,
.source-card__actions {
  flex-wrap: wrap;
}

.source-card p,
.citation-item div,
.retrieval-snippet,
.retrieval-meta {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  color: var(--it-text-muted);
}

.debug-row {
  display: grid;
  grid-template-columns: 1fr 110px auto;
  gap: 10px;
  align-items: center;
}

.debug-state {
  margin: 12px 0 0;
  padding: 10px;
  border-radius: 8px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
  font-size: 12px;
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
  min-width: 560px;
  max-width: calc(100vw - 24px);
  background: var(--it-surface-solid);
  box-shadow: var(--it-shadow-strong, var(--it-shadow));
  z-index: 2600;
}

.global-ai-assistant ::v-deep .el-textarea__inner,
.global-ai-assistant ::v-deep .el-input__inner,
.global-ai-assistant ::v-deep .el-button {
  border-radius: 8px;
}

.ai-main::-webkit-scrollbar,
.session-list::-webkit-scrollbar,
.chat-body::-webkit-scrollbar {
  width: 8px;
}

.ai-main::-webkit-scrollbar-thumb,
.session-list::-webkit-scrollbar-thumb,
.chat-body::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.45);
  border-radius: 8px;
}

@media (max-width: 900px) {
  .ai-workspace {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .ai-session-card {
    max-height: 180px;
  }

  .control-grid,
  .debug-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .global-ai-assistant ::v-deep .ai-drawer {
    min-width: 0;
    max-width: 100vw;
  }

  .chat-body {
    max-height: 46vh;
  }

  .chat-item__content {
    max-width: 100%;
  }
}
</style>
