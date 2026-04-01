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

          
          <div v-if="selectedKnowledgeBaseId" class="ai-card ai-panel__debug">
            <div class="section-title section-title--between">
              <span>检索调试</span>
              <el-tag size="mini" type="info" effect="plain">当前知识库</el-tag>
            </div>
            <div class="debug-row">
              <el-input
                v-model.trim="debugQuery"
                size="small"
                clearable
                placeholder="输入调试问题；不填时默认使用当前输入框内容"
              />
              <el-input-number v-model="debugTopK" :min="1" :max="10" size="small" controls-position="right" />
              <el-button size="small" :loading="debugLoading" @click="runKnowledgeBaseDebugSearch">调试检索</el-button>
            </div>
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
                <div v-if="msg.role === 'assistant'" class="chat-item__footer">
                  <el-button
                    v-if="msg.sources && msg.sources.length"
                    type="text"
                    size="mini"
                    @click="toggleMessageSources(index)"
                  >
                    {{ msg.sourceOpen ? '收起来源' : `引用来源（${msg.sources.length}）` }}
                  </el-button>
                  <el-button
                    v-if="msg.callLogId"
                    type="text"
                    size="mini"
                    @click="openRetrievalDrawerByMessage(msg)"
                  >
                    检索日志
                  </el-button>
                </div>
                <div
                  v-if="msg.role === 'assistant' && msg.sources && msg.sources.length && msg.sourceOpen"
                  class="chat-item__sources"
                >
                  <div
                    v-for="(source, sIndex) in msg.sources"
                    :key="source.id || `${index}-${sIndex}`"
                    class="source-card"
                  >
                    <div class="source-card__top">
                      <div class="source-card__title">{{ source.title || '未命中文档标题' }}</div>
                      <div class="source-card__meta">
                        <span v-if="source.score !== null && source.score !== undefined">Score {{ source.score }}</span>
                        <span v-if="source.keywordScore !== null && source.keywordScore !== undefined">关键词 {{ source.keywordScore }}</span>
                        <span v-if="source.vectorScore !== null && source.vectorScore !== undefined">向量 {{ source.vectorScore }}</span>
                        <span v-if="source.chunkIndex !== null && source.chunkIndex !== undefined">Chunk #{{ source.chunkIndex }}</span>
                        <span v-if="source.retrievalMethod">{{ source.retrievalMethod }}</span>
                      </div>
                    </div>
                    <div v-if="source.knowledgeBaseName" class="source-card__kb">知识库：{{ source.knowledgeBaseName }}</div>
                    <div v-if="source.archiveEntryPath || source.fileName" class="source-card__path">
                      {{ source.archiveEntryPath || source.fileName }}
                    </div>
                    <div class="source-card__actions">
                      <el-button type="text" size="mini" @click="locateSourceDocument(source)">定位知识库</el-button>
                      <el-button v-if="source.callLogId" type="text" size="mini" @click="openRetrievalDrawer(source.callLogId, source.title || '检索日志')">
                        检索日志
                      </el-button>
                    </div>
                    <div class="source-card__content">{{ source.content || '暂无切片内容' }}</div>
                  </div>
                </div>
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

    <el-dialog
      title="检索日志 / 调试结果"
      :visible.sync="retrievalDrawerVisible"
      width="980px"
      append-to-body
      destroy-on-close
    >
      <div class="retrieval-meta">{{ currentRetrievalMeta }}</div>
      <el-table :data="retrievalLogs" v-loading="debugLoading" border stripe size="small">
        <el-table-column prop="title" label="命中文档" min-width="200" show-overflow-tooltip />
        <el-table-column prop="documentId" label="文档 ID" width="100" />
        <el-table-column prop="chunkIndex" label="Chunk" width="90" />
        <el-table-column prop="score" label="Score" width="100" />
        <el-table-column prop="keywordScore" label="关键词分" width="100" />
        <el-table-column prop="vectorScore" label="向量分" width="100" />
        <el-table-column prop="retrievalMethod" label="检索方式" width="120" />
        <el-table-column label="路径" min-width="220" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.archiveEntryPath || row.fileName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="320">
          <template slot-scope="{ row }">
            <div class="retrieval-snippet">{{ row.content || row.snippet || '-' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

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
  pageKnowledgeBasesByOwner,
  listCallRetrievals,
  searchKnowledgeBaseDebug
} from '@/api/knowledgeBase'

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
      selectedKnowledgeBaseLocked: false,
      selectedKnowledgeBaseLockSource: '',
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
      retrievalDrawerVisible: false,
      retrievalLogs: [],
      currentRetrievalMeta: '',
      debugQuery: '',
      debugTopK: 5,
      debugLoading: false,
      messages: [
        {
          role: 'assistant',
          content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。',
          sources: [],
          sourceOpen: false,
          callLogId: null
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

    normalizeSources(list = [], extra = {}) {
      if (!Array.isArray(list)) return []
      return list.map(item => {
        const document = item.document || {}
        const chunk = item.chunk || {}
        const knowledgeBase = item.knowledgeBase || {}
        return {
          id: item.id || item.chunkId || chunk.id || item.documentId || document.id || null,
          callLogId: extra.callLogId || item.callLogId || null,
          title: item.title || item.documentTitle || item.documentName || document.title || '',
          documentId: item.documentId || document.id || null,
          knowledgeBaseId: item.knowledgeBaseId || knowledgeBase.id || this.selectedKnowledgeBaseId || null,
          chunkId: item.chunkId || chunk.id || null,
          knowledgeBaseName: item.knowledgeBaseName || item.kbName || knowledgeBase.name || this.selectedKnowledgeBaseLabel || '',
          chunkIndex:
            item.chunkIndex !== undefined && item.chunkIndex !== null
              ? item.chunkIndex
              : chunk.chunkIndex !== undefined && chunk.chunkIndex !== null
                ? chunk.chunkIndex
                : item.rankNo !== undefined && item.rankNo !== null
                  ? item.rankNo
                  : null,
          rankNo: item.rankNo !== undefined && item.rankNo !== null ? item.rankNo : null,
          score: item.score !== undefined ? item.score : null,
          keywordScore: item.keywordScore !== undefined ? item.keywordScore : null,
          vectorScore: item.vectorScore !== undefined ? item.vectorScore : null,
          retrievalMethod: item.retrievalMethod || item.method || '',
          fileName: item.fileName || document.fileName || '',
          archiveEntryPath: item.archiveEntryPath || item.path || document.archiveEntryPath || '',
          snippet: item.snippet || '',
          content: item.content || item.chunkContent || item.text || item.snippet || chunk.content || ''
        }
      })
    },

    extractSources(data, extra = {}) {
      if (!data || typeof data !== 'object') return []
      const sourceList = data.sources || data.citations || data.references || data.retrievals || data.retrievedChunks || data.hits || []
      return this.normalizeSources(sourceList, extra)
    },

    extractAssistantText(data) {
      if (!data) return ''
      return data.answer || data.content || data.message || data.responseText || data.text || ''
    },

    async ensureAssistantMessageSourceLoaded(message) {
      if (!message || !message.callLogId || (Array.isArray(message.sources) && message.sources.length)) return
      try {
        const res = await listCallRetrievals(message.callLogId)
        const data = this.extractResponseData(res)
        const list = Array.isArray(data) ? data : (data && (data.content || data.list || data.records)) || []
        this.$set(message, 'sources', this.normalizeSources(list, { callLogId: message.callLogId }))
      } catch (e) {}
    },

    toggleMessageSources(index) {
      const msg = this.messages[index]
      if (!msg) return
      const next = !msg.sourceOpen
      this.$set(msg, 'sourceOpen', next)
      if (next) {
        this.ensureAssistantMessageSourceLoaded(msg)
      }
    },

    locateSourceDocument(source) {
      if (!source || !source.documentId) {
        this.$message.info('当前来源没有文档 ID')
        return
      }
      const kbId = source.knowledgeBaseId || this.selectedKnowledgeBaseId || ''
      this.visible = false
      this.$router.push({
        path: '/knowledge-base',
        query: {
          kbId,
          documentId: source.documentId
        }
      })
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
      try {
        const res = await listCallRetrievals(callLogId)
        const data = this.extractResponseData(res)
        const list = Array.isArray(data) ? data : (data && (data.content || data.list || data.records)) || []
        this.retrievalLogs = this.normalizeSources(list, { callLogId })
      } catch (e) {
        this.$message.error(extractErrorMessage(e, '加载检索日志失败'))
      } finally {
        this.debugLoading = false
      }
    },

    async runKnowledgeBaseDebugSearch() {
      if (!this.selectedKnowledgeBaseId) {
        this.$message.warning('请先选择知识库')
        return
      }
      const query = (this.debugQuery || this.input || '').trim()
      if (!query) {
        this.$message.warning('请输入调试问题')
        return
      }
      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `检索调试：${query}`
      this.debugLoading = true
      try {
        const res = await searchKnowledgeBaseDebug(this.selectedKnowledgeBaseId, {
          query,
          topK: this.debugTopK || 5
        })
        const payload = this.extractResponseData(res) || {}
        const hits = Array.isArray(payload.hits) ? payload.hits : this.extractSources(payload)
        this.retrievalLogs = this.normalizeSources(hits, {
          knowledgeBaseId: this.selectedKnowledgeBaseId
        })
      } catch (e) {
        this.$message.error(extractErrorMessage(e, '检索调试失败'))
      } finally {
        this.debugLoading = false
      }
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

    setKnowledgeBaseSelection(value, options = {}) {
      const { resetSession = false, manual = false, source = '' } = options
      const nextId = value ? Number(value) || null : null
      const currentId = this.selectedKnowledgeBaseId ? Number(this.selectedKnowledgeBaseId) || null : null
      const changed = currentId !== nextId
      this.selectedKnowledgeBaseId = nextId
      this.selectedKnowledgeBaseLocked = !!(manual && nextId)
      this.selectedKnowledgeBaseLockSource = manual && nextId ? (source || 'manual') : ''
      if (resetSession && changed) {
        this.sessionId = null
        this.messages = [
          {
            role: 'assistant',
            content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。',
            sources: [],
            sourceOpen: false,
            callLogId: null
          }
        ]
        this.$nextTick(this.scrollToBottom)
      }
      if (typeof window !== 'undefined') {
        if (nextId) {
          localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(nextId))
        } else {
          localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
        }
      }
    },

    syncSceneKnowledgeBaseSelection() {
      const sceneKb = this.readCurrentKnowledgeBase()
      const path = (this.$route && this.$route.path) || ''
      const currentId = this.selectedKnowledgeBaseId ? Number(this.selectedKnowledgeBaseId) || null : null

      if (sceneKb && sceneKb.id) {
        this.ensureKnowledgeBaseOption(sceneKb)
      }

      if (path.includes('/knowledge-base') && sceneKb && sceneKb.id) {
        const sceneId = Number(sceneKb.id) || null
        if (!currentId) {
          this.setKnowledgeBaseSelection(sceneId, { manual: false, source: 'scene-init' })
          return
        }
        if (!this.selectedKnowledgeBaseLocked && currentId !== sceneId) {
          this.setKnowledgeBaseSelection(sceneId, { manual: false, source: 'scene-sync' })
        }
        return
      }

      const remembered = this.readSelectedKnowledgeBaseId()
      if (!currentId && remembered) {
        this.setKnowledgeBaseSelection(remembered, { manual: true, source: 'remembered' })
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
      const path = ((this.$route && this.$route.path) || '')
      if (!path.includes('/knowledge-base')) return
      if (kb && kb.id) {
        this.ensureKnowledgeBaseOption(kb)
        if (!this.selectedKnowledgeBaseLocked && !this.selectedKnowledgeBaseId) {
          this.setKnowledgeBaseSelection(kb.id, { manual: false, source: 'scene-event' })
        }
      } else if (!this.selectedKnowledgeBaseLocked) {
        this.setKnowledgeBaseSelection(null, { manual: false, source: 'scene-clear' })
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
      this.setKnowledgeBaseSelection(val, { resetSession: true, manual: true, source: 'user-select' })
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
      this.selectedKnowledgeBaseLocked = false
      this.selectedKnowledgeBaseLockSource = ''
      this.messages = [
        {
          role: 'assistant',
          content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。',
          sources: [],
          sourceOpen: false,
          callLogId: null
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

    buildPayload(question, lockedKnowledgeBaseId = null) {
      const kbId = lockedKnowledgeBaseId ? Number(lockedKnowledgeBaseId) || null : (this.selectedKnowledgeBaseId ? Number(this.selectedKnowledgeBaseId) || null : null)
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
        knowledgeBaseIds: kbId ? [kbId] : [],
        defaultKnowledgeBaseId: kbId
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
      const lockedKnowledgeBaseId = this.selectedKnowledgeBaseId ? Number(this.selectedKnowledgeBaseId) || null : null
      const payload = this.buildPayload(question, lockedKnowledgeBaseId)
      console.log('AI payload =>', payload)
      this.messages.push({ role: 'user', content: question })
      this.input = ''
      this.sending = true

      const assistantMessage = {
        role: 'assistant',
        content: '正在思考...',
        sources: [],
        sourceOpen: false,
        callLogId: null,
        modelName: ''
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
        this.ensureAssistantMessageSourceLoaded(assistantMessage)
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
          assistantMessage.modelName = chunk.modelName
        }
        if (chunk.callLogId) {
          assistantMessage.callLogId = chunk.callLogId
        }
        const chunkSources = this.extractSources(chunk, { callLogId: chunk.callLogId || assistantMessage.callLogId })
        if (chunkSources.length) {
          assistantMessage.sources = chunkSources
          assistantMessage.sourceOpen = true
        }
        const chunkText = this.extractAssistantText(chunk)
        if (!chunk.delta && chunkText && !hasChunk) {
          partialText = chunkText
          assistantMessage.content = partialText
          hasChunk = true
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
              assistantMessage.modelName = data.modelName
            }
            if (data.callLogId) {
              assistantMessage.callLogId = data.callLogId
            }
            const finalSources = this.extractSources(data, { callLogId: data.callLogId || assistantMessage.callLogId })
            if (finalSources.length) {
              assistantMessage.sources = finalSources
              assistantMessage.sourceOpen = true
            }
            assistantMessage.content = this.extractAnswer(data) || '已完成，但没有返回内容'
            this.ensureAssistantMessageSourceLoaded(assistantMessage)
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


.section-title--between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-panel__debug {
  display: grid;
  gap: 10px;
}

.debug-row {
  display: grid;
  grid-template-columns: 1fr 110px auto;
  gap: 10px;
  align-items: center;
}

.chat-item__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.chat-item__sources {
  margin-top: 10px;
  display: grid;
  gap: 10px;
}

.source-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #f8fafc;
  border-radius: 14px;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.source-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.source-card__title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.source-card__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 12px;
}

.source-card__kb,
.source-card__path {
  font-size: 12px;
  color: #475569;
  word-break: break-all;
}

.source-card__actions {
  display: flex;
  gap: 10px;
}

.source-card__content,
.retrieval-snippet,
.retrieval-meta {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  color: #334155;
}

.retrieval-meta {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .nav-grid {
    grid-template-columns: 1fr;
  }

  .debug-row {
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
