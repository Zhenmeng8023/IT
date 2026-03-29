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
      size="420px"
      :with-header="false"
      custom-class="ai-drawer"
    >
      <div class="ai-panel">
        <div class="ai-panel__header">
          <div>
            <div class="ai-panel__title">全局 AI 助手</div>
            <div class="ai-panel__subtitle">通用问答、业务导航、上下文提示</div>
          </div>
          <el-button type="text" icon="el-icon-close" @click="visible = false" />
        </div>

        <div class="ai-panel__scene">
          <el-tag size="mini" effect="plain">{{ sceneLabel }}</el-tag>

          <el-tag
            v-if="selectedKnowledgeBaseLabel"
            size="mini"
            type="success"
            effect="plain"
          >
            知识库：{{ selectedKnowledgeBaseLabel }}
          </el-tag>

          <el-tag
            v-if="activeModelName"
            size="mini"
            type="warning"
            effect="plain"
          >
            当前模型：{{ activeModelName }}
          </el-tag>
        </div>

        <div class="ai-panel__nav">
          <div class="section-title">快捷导航</div>
          <div class="nav-grid">
            <div
              v-for="item in quickNavs"
              :key="item.path"
              class="nav-card"
              @click="go(item.path)"
            >
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="ai-panel__kb">
          <div class="section-title">关联知识库（可选）</div>
          <el-select
            v-model="selectedKnowledgeBaseId"
            clearable
            filterable
            size="small"
            placeholder="选择知识库增强回答"
            style="width: 100%;"
            :popper-append-to-body="false"
            popper-class="ai-kb-select-popper"
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

        <div class="ai-panel__chat">
          <div class="section-title">对话</div>

          <div ref="chatBody" class="chat-body">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="chat-item"
              :class="msg.role"
            >
              <div class="chat-item__role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
              <div class="chat-item__content">{{ msg.content }}</div>
            </div>
          </div>

          <div class="chat-quick-actions">
            <el-button size="mini" @click="fillPrompt('帮我说明一下当前页面可以做什么')">
              当前页面能做什么
            </el-button>
            <el-button size="mini" @click="fillPrompt('给我下一步操作建议')">
              下一步建议
            </el-button>
            <el-button size="mini" @click="fillPrompt('帮我导航到知识库管理台')">
              导航到知识库
            </el-button>
          </div>

          <el-input
            v-model.trim="input"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="输入问题，或让 AI 帮你导航到某个业务页面"
            @keyup.native.ctrl.enter="send"
          />

          <div class="chat-actions">
            <el-button @click="clearChat">清空</el-button>
            <el-button type="primary" :loading="sending" @click="send">
              {{ sending ? '生成中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { aiChatTurn } from '@/api/aiAssistant'
import { getActiveAiModel } from '@/api/aiAdmin'
import { pageKnowledgeBasesByOwner } from '@/api/knowledgeBase'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'

export default {
  name: 'AIAssistant',
  data() {
    return {
      visible: false,
      input: '',
      sending: false,
      sessionId: null,
      selectedKnowledgeBaseId: null,
      knowledgeBaseOptions: [],
      activeModelId: null,
      activeModelName: '',
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
      try {
        const raw =
          localStorage.getItem('userInfo') ||
          localStorage.getItem('user') ||
          sessionStorage.getItem('userInfo') ||
          sessionStorage.getItem('user')
        if (!raw) return 1
        const user = JSON.parse(raw)
        return user && (user.id || user.userId) ? (user.id || user.userId) : 1
      } catch (e) {
        return 1
      }
    },
    currentSceneKnowledgeBase() {
      return this.readCurrentKnowledgeBase()
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
          label: this.currentSceneKnowledgeBase && this.currentSceneKnowledgeBase.name
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
      const hit = this.knowledgeBaseOptions.find(item => item.id === this.selectedKnowledgeBaseId)
      if (hit && hit.name) return hit.name
      if (
        this.currentSceneKnowledgeBase &&
        this.currentSceneKnowledgeBase.id === this.selectedKnowledgeBaseId
      ) {
        return this.currentSceneKnowledgeBase.name || `知识库 #${this.selectedKnowledgeBaseId}`
      }
      return `知识库 #${this.selectedKnowledgeBaseId}`
    },
    quickNavs() {
      return [
        { label: '首页', path: '/', icon: 'el-icon-house' },
        { label: '项目列表', path: '/projectlist', icon: 'el-icon-folder-opened' },
        { label: '写博客', path: '/blogwrite', icon: 'el-icon-edit-outline' },
        { label: '博客广场', path: '/blog', icon: 'el-icon-document' },
        { label: '知识库', path: '/knowledge-base', icon: 'el-icon-notebook-2' }
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
      if (val) {
        localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
      }
    }
  },
  mounted() {
    window.addEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    this.syncSceneKnowledgeBaseSelection()
  },
  beforeDestroy() {
    window.removeEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
  },
  methods: {
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

    extractAnswer(data) {
      if (!data) return ''
      return data.answer || data.content || data.message || data.responseText || ''
    },

    readCurrentKnowledgeBase() {
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
      const raw = localStorage.getItem(SELECTED_KB_STORAGE_KEY)
      return raw ? Number(raw) || null : null
    },

    ensureKnowledgeBaseOption(kb) {
      if (!kb || !kb.id) return
      const normalized = this.normalizeKnowledgeBase(kb)
      const exists = this.knowledgeBaseOptions.some(item => item.id === normalized.id)
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
      await Promise.all([this.loadKnowledgeBases(), this.loadActiveModel()])
      this.syncSceneKnowledgeBaseSelection()
      this.$nextTick(this.scrollToBottom)
    },

    async openDrawer() {
      this.visible = true
      await this.initializeAssistant()
    },

    async loadKnowledgeBases() {
      try {
        const res = await pageKnowledgeBasesByOwner(this.userId, { page: 0, size: 50 })
        const pageData = this.extractPageData(res)
        this.knowledgeBaseOptions = (pageData.content || []).map(this.normalizeKnowledgeBase)

        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) {
          this.ensureKnowledgeBaseOption(sceneKb)
        }

        console.log('knowledgeBaseOptions =', this.knowledgeBaseOptions)
      } catch (e) {
        this.knowledgeBaseOptions = []
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) {
          this.knowledgeBaseOptions = [this.normalizeKnowledgeBase(sceneKb)]
        }
        console.error('loadKnowledgeBases error =', e)
      }
    },

    async loadActiveModel() {
      try {
        const res = await getActiveAiModel()
        const payload = this.extractResponseData(res) || {}
        this.activeModelId = payload.id || null
        this.activeModelName =
          payload.modelName || payload.name || payload.displayName || payload.providerModel || ''
      } catch (e) {
        this.activeModelId = null
        this.activeModelName = ''
      }
    },

    handleKnowledgeBaseChange(val) {
      if (val) {
        localStorage.setItem(SELECTED_KB_STORAGE_KEY, String(val))
      } else {
        localStorage.removeItem(SELECTED_KB_STORAGE_KEY)
      }
    },

    go(path) {
      this.visible = false
      if (path === '/knowledge-base' && !this.canOpenKnowledgeBase()) {
        this.$message.warning('当前账号不属于知识库管理台开放范围')
        return
      }
      this.$router.push(path)
    },

    canOpenKnowledgeBase() {
      const checker = this.$hasPermission
      if (typeof checker === 'function') {
        return (
          checker('view:admin:dashboard') ||
          checker('view:admin:system-log') ||
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
      this.sending = false
      this.sessionId = null
      this.messages = [
        {
          role: 'assistant',
          content: '你好，我是全局 AI 助手。你可以直接问我问题，也可以让我帮你导航到项目、博客或知识库页面。'
        }
      ]
      this.input = ''
    },

    buildPayload(question) {
      return {
        sessionId: this.sessionId,
        userId: this.userId,
        content: question,
        modelId: this.activeModelId || null,
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
      if (!this.input) {
        this.$message.warning('请输入问题')
        return
      }
      if (this.sending) return

      const question = this.input
      this.messages.push({ role: 'user', content: question })
      this.input = ''
      this.sending = true
      this.$nextTick(this.scrollToBottom)

      try {
        await this.loadActiveModel()

        const res = await aiChatTurn(this.buildPayload(question))
        const payload = this.extractResponseData(res) || {}

        if (payload.sessionId) {
          this.sessionId = payload.sessionId
        }
        if (payload.modelId) {
          this.activeModelId = payload.modelId
        }
        if (payload.modelName) {
          this.activeModelName = payload.modelName
        }

        const answer = this.extractAnswer(payload) || '已完成，但没有返回内容'
        this.messages.push({
          role: 'assistant',
          content: answer
        })
      } catch (e) {
        this.messages.push({
          role: 'assistant',
          content: '请求失败，请稍后重试'
        })
      } finally {
        this.sending = false
        this.$nextTick(this.scrollToBottom)
      }
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
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #6a8dff);
  color: #fff;
  box-shadow: 0 12px 30px rgba(64, 158, 255, 0.28);
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
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  box-sizing: border-box;
  background: #f8fafc;
}

.ai-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.ai-panel__title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.ai-panel__subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.ai-panel__scene,
.ai-panel__nav,
.ai-panel__kb,
.ai-panel__chat {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.nav-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-card:hover {
  border-color: #409eff;
  transform: translateY(-1px);
}

.chat-body {
  height: 320px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
}

.chat-item {
  margin-bottom: 12px;
}

.chat-item__role {
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.chat-item__content {
  display: inline-block;
  max-width: 90%;
  padding: 10px 12px;
  border-radius: 10px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.chat-item.user .chat-item__content {
  background: #409eff;
  color: #fff;
}

.chat-item.assistant .chat-item__content {
  background: #f5f7fa;
  color: #303133;
}

.chat-quick-actions {
  margin: 10px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-actions {
  margin-top: 10px;
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

.ai-panel__kb {
  position: relative;
  z-index: 30;
  overflow: visible;
}

.global-ai-assistant ::v-deep .el-drawer__body {
  overflow: visible;
}

.global-ai-assistant ::v-deep .el-select-dropdown {
  z-index: 10000 !important;
}

.global-ai-assistant ::v-deep .ai-kb-select-popper {
  z-index: 10000 !important;
}
</style>