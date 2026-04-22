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
      :modal="false"
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
            <el-switch v-if="canToggleDeveloperMode" v-model="developerMode" active-text="开发" inactive-text="用户" @change="persistDeveloperMode" />
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
                  <span>对话模式</span>
                  <el-radio-group v-model="assistantMode" size="mini" @change="handleAssistantModeChange">
                    <el-radio-button label="GENERAL_CHAT">通用对话</el-radio-button>
                    <el-radio-button label="KNOWLEDGE_QA" :disabled="!canUseKnowledgeMode">知识库问答</el-radio-button>
                  </el-radio-group>
                </label>
                <label v-if="knowledgeFeatureEnabledByScene" class="control-field">
                  <span>知识库</span>
                  <el-select
                    v-model="selectedKnowledgeBaseIds"
                    multiple
                    collapse-tags
                    clearable
                    filterable
                    :disabled="knowledgeSelectorDisabled"
                    size="small"
                    :placeholder="canUseKnowledgeMode ? '选择知识库（知识库问答模式生效）' : '当前账号不可用'"
                    @change="handleKnowledgeBaseChange"
                  >
                    <el-option v-for="item in knowledgeBaseOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </label>
                <label class="control-field">
                  <span>分析模式</span>
                  <el-select v-model="analysisMode" size="small" @change="handleAnalysisModeChange">
                    <el-option v-for="item in analysisModeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </label>
                <label class="control-field control-field--inline">
                  <span>Strict Grounding</span>
                  <el-switch v-model="strictGrounding" @change="handleStrictGroundingChange" />
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
                <div v-if="showSceneEmptyState" class="ai-scene-empty">
                  <div class="ai-scene-empty__title">当前场景可做事项</div>
                  <div class="ai-scene-empty__desc">可以直接点下面推荐，或自己输入更具体的问题。</div>
                  <ul class="ai-scene-empty__list">
                    <li v-for="item in sceneSuggestions" :key="`${item.label}-${item.actionCode || item.prompt}`">
                      {{ item.description || item.label }}
                    </li>
                  </ul>
                </div>
                <article v-for="msg in messages" :key="msg.id" class="chat-item" :class="[msg.role, { 'has-error': msg.errorType }]">
                  <div class="chat-item__role">
                    <span>{{ msg.role === 'user' ? '我' : 'AI' }}</span>
                    <small v-if="msg.status">{{ msg.status }}</small>
                  </div>
                  <div class="chat-item__content" v-html="renderMessage(msg)"></div>
                  <div v-if="msg.role === 'assistant' && isInsufficientContextMessage(msg)" class="chat-item__insufficient">
                    <div v-if="msg.insufficientContext && msg.insufficientContext.coverage" class="chat-item__insufficient-meta">
                      覆盖范围：{{ formatStructuredCoverage(msg.insufficientContext.coverage) }}
                    </div>
                    <div v-if="msg.insufficientContext && msg.insufficientContext.nextAction" class="chat-item__insufficient-meta">
                      建议动作：{{ msg.insufficientContext.nextAction }}
                    </div>
                    <div class="chat-item__insufficient-actions">
                      <el-button size="mini" @click="handleInsufficientContextCta('continue_current_page', msg)">仅基于当前页面继续</el-button>
                      <el-button size="mini" @click="handleInsufficientContextCta('read_more_project_docs', msg)">读取更多项目文档再试</el-button>
                      <el-button size="mini" @click="handleInsufficientContextCta('import_knowledge_base', msg)">去知识库导入资料</el-button>
                    </div>
                  </div>
                  <div v-if="msg.errorMessage" class="chat-item__error">{{ msg.errorMessage }}</div>
                  <div v-if="msg.role === 'assistant'" class="chat-item__footer">
                    <el-button v-if="false && msg.sources && msg.sources.length" type="text" size="mini" @click="toggleMessageSources(msg)">
                      {{ msg.sourceOpen ? '收起来源' : `引用来源（${msg.sources.length}）` }}
                    </el-button>
                    <el-button v-if="showDeveloperPanel && msg.callLogId" type="text" size="mini" @click="openRetrievalDrawerByMessage(msg)">
                      检索日志
                    </el-button>
                  </div>
                  <div v-if="false && msg.role === 'assistant' && msg.sources && msg.sources.length && msg.sourceOpen" class="chat-item__sources">
                    <div v-for="(source, index) in msg.sources" :key="source.id || `${msg.id}-${index}`" class="source-card">
                      <div class="source-card__top">
                        <strong>{{ source.documentTitle || source.title || '未命中文档标题' }}</strong>
                        <span v-if="source.score !== null && source.score !== undefined">Score {{ source.score }}</span>
                      </div>
                      <div class="source-card__meta">
                        <span v-if="source.knowledgeBaseName">知识库：{{ source.knowledgeBaseName }}</span>
                        <span v-else-if="source.knowledgeBaseId">知识库 #{{ source.knowledgeBaseId }}</span>
                        <span v-if="source.documentId">文档 #{{ source.documentId }}</span>
                        <span v-if="source.chunkId">Chunk #{{ source.chunkId }}</span><span v-if="source.stageCode">阶段：{{ source.stageCode }}</span>
                        <span v-if="source.reason">命中原因：{{ source.reason }}</span>
                      </div>
                      <div class="source-card__actions">
                        <el-button type="text" size="mini" @click="locateSourceDocument(source)">定位知识库</el-button>
                        <el-button v-if="showDeveloperPanel && source.callLogId" type="text" size="mini" @click="openRetrievalDrawer(source.callLogId, source.title || '检索日志')">检索日志</el-button>
                      </div>
                      <p>{{ source.content || source.snippet || '暂无切片内容' }}</p>
                    </div>
                  </div>
                </article>
              </div>
              <div v-if="sceneQuickPrompts.length" class="chat-quick-actions">
                <el-button
                  v-for="item in sceneQuickPrompts"
                  :key="`${item.label}-${item.prompt}`"
                  size="mini"
                  round
                  @click="fillPrompt(item.prompt)"
                >
                  {{ item.label }}
                </el-button>
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

          </section>

          <aside class="ai-insight">
            <div class="ai-card ai-knowledge-card">
              <div class="section-title section-title--between">
                <span>知识库</span>
                <span class="muted-text">{{ assistantMode === 'KNOWLEDGE_QA' ? '知识库问答' : '通用对话' }}</span>
              </div>
              <el-alert v-if="areaErrors.knowledge" class="area-alert" type="warning" :closable="false" :title="areaErrors.knowledge" show-icon />
              <div class="muted-text">{{ knowledgePanelStatusText }}</div>
              <div v-if="selectedKnowledgeBaseLabels.length" class="knowledge-selected-list">
                <el-tag v-for="label in selectedKnowledgeBaseLabels" :key="`selected-${label}`" size="mini" type="success" effect="plain">{{ label }}</el-tag>
              </div>
              <div v-else class="empty-small">当前未绑定知识库</div>
            </div>

            <div class="ai-card ai-citation-card">
              <div class="section-title section-title--between">
                <span>引用</span>
                <span class="muted-text">{{ visibleCitations.length ? `${visibleCitations.length} 条` : '暂无引用' }}</span>
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

            <div class="ai-card ai-evidence-card">
              <div class="section-title section-title--between">
                <span>证据</span>
                <span class="muted-text">{{ visibleEvidence ? '已关联检索证据' : '暂无证据摘要' }}</span>
              </div>
              <div v-if="visibleEvidence" class="evidence-grid">
                <div class="evidence-item">
                  <strong>Grounding</strong>
                  <span>{{ visibleGroundingStatusText }}</span>
                </div>
                <div class="evidence-item">
                  <strong>Strict</strong>
                  <span>{{ visibleStrictGroundingText }}</span>
                </div>
                <div class="evidence-item">
                  <strong>Declaration 命中</strong>
                  <span>{{ visibleDeclarationHitCount }}</span>
                </div>
                <div class="evidence-item">
                  <strong>Graph Expand 命中</strong>
                  <span>{{ visibleGraphExpandHitCount }}</span>
                </div>
                <div class="evidence-item">
                  <strong>Degraded</strong>
                  <span>{{ visibleDegradedText }}</span>
                </div>
              </div>
              <div v-if="visibleDegradeReason" class="evidence-reason">降级原因：{{ visibleDegradeReason }}</div>
              <div v-if="visibleEvidenceHits.length" class="evidence-hit-list">
                <div v-for="(item, index) in visibleEvidenceHits" :key="`${item.chunkId || 'hit'}-${index}`" class="evidence-hit-item">
                  <div class="evidence-hit-item__head">
                    <strong>#{{ item.rankNo || index + 1 }}</strong>
                    <span>{{ item.stageCode || item.phase || '-' }}</span>
                    <span v-if="item.reason">{{ item.reason }}</span>
                  </div>
                  <div class="evidence-hit-item__meta">
                    <span v-if="item.chunkId">Chunk #{{ item.chunkId }}</span>
                    <span v-if="item.path">{{ item.path }}</span>
                    <span v-if="item.symbolName">{{ item.symbolType || 'symbol' }} {{ item.symbolName }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-small">暂无可解释证据命中</div>
            </div>

            <div v-if="showDeveloperPanel" class="ai-card ai-debug-card">
              <div class="section-title section-title--between">
                <span>调试</span>
                <el-tag size="mini" type="info" effect="plain">仅管理员/开发模式</el-tag>
              </div>
              <el-alert v-if="areaErrors.debug" class="area-alert" type="error" :closable="false" :title="areaErrors.debug" show-icon />
              <div class="debug-row">
                <el-input v-model.trim="debugQuery" size="small" clearable placeholder="输入调试问题；不填时默认使用当前输入框内容" />
                <el-input-number v-model="debugTopK" :min="1" :max="10" size="small" controls-position="right" />
                <el-button size="small" :loading="debugLoading" @click="runKnowledgeBaseDebugSearch">调试检索</el-button>
              </div>
              <pre class="debug-state">{{ debugStateText }}</pre>
            </div>
          </aside>
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
            <el-table-column prop="stageCode" label="阶段" width="150" />
            <el-table-column prop="reason" label="命中原因" min-width="180" show-overflow-tooltip />
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
  pageMyKnowledgeBases,
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
import { normalizeAiAssistantOpenPayload } from '@/utils/aiOpenPayload'
import { isValidAnalysisMode, resolveSceneAnalysisMode, saveSceneAnalysisMode } from '@/utils/aiModePreference'
import { resolveAiSceneMeta } from '@/utils/aiSceneRegistry'
import { getAiSuggestionPreset } from '@/utils/aiSuggestionPreset'
import { getAiUxMetricsSnapshot, recordAiUxMetric } from '@/utils/aiUxMetrics'
import { summarizeAiContextPayload } from '@/utils/aiContextCollectors'

const CURRENT_KB_STORAGE_KEY = 'ai_assistant_current_kb'
const SELECTED_KB_STORAGE_KEY = 'ai_assistant_selected_kb_id'
const SELECTED_KBS_STORAGE_KEY = 'ai_assistant_selected_kb_ids'
const SELECTED_MODEL_STORAGE_KEY = 'ai_assistant_selected_model_id'
const DRAWER_WIDTH_STORAGE_KEY = 'ai_assistant_drawer_width'
const CHAT_HEIGHT_STORAGE_KEY = 'ai_assistant_chat_height'
const DEV_MODE_STORAGE_KEY = 'ai_assistant_dev_mode'
const STRICT_GROUNDING_STORAGE_KEY = 'ai_assistant_strict_grounding'
const FORCE_DEBUG_STORAGE_KEY = 'ai_assistant_force_debug'
const ASSISTANT_MODE_GENERAL = 'GENERAL_CHAT'
const ASSISTANT_MODE_KNOWLEDGE = 'KNOWLEDGE_QA'
const AUTH_STORAGE_KEYS = ['permissions', 'permissionCodes', 'authorities', 'roles', 'menus', 'userPermissions', 'userInfo', 'user', 'loginUser']
const KB_AUTHORITY_HINTS = [
  'view:front:ai:kb:self',
  'edit:front:ai:kb:self',
  'view:front:ai:kb:project',
  'edit:front:ai:kb:project',
  'manage:front:ai:kb:member',
  'view:knowledge-base',
  'manage:knowledge-base',
  'knowledge-base',
  'knowledge_base',
  'ai:knowledge'
]
const DEBUG_AUTHORITY_HINTS = ['view:admin', 'manage:admin', 'role:admin', 'admin', 'developer', 'dev', 'root', 'super']

function safeParsePermissionPayload(raw) {
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function appendPermissionCodes(target, source) {
  if (!source) return
  if (Array.isArray(source)) {
    source.forEach(item => appendPermissionCodes(target, item))
    return
  }
  if (typeof source === 'string') {
    const code = source.trim()
    if (code) target.add(code)
    return
  }
  if (typeof source !== 'object') return

  ;[
    source.permissionCode,
    source.permission,
    source.code,
    source.authority,
    source.value,
    source.name
  ].forEach(item => appendPermissionCodes(target, item))

  ;[
    source.permissions,
    source.permissionCodes,
    source.authorities,
    source.roles,
    source.menus,
    source.children,
    source.buttonPermissions,
    source.roleCodes
  ].forEach(item => appendPermissionCodes(target, item))
}

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
      analysisMode: 'DOC_QA',
      strictGrounding: false,
      assistantMode: ASSISTANT_MODE_GENERAL,
      analysisModeOptions: [
        { value: 'DOC_QA', label: '文档问答 (DOC_QA)' },
        { value: 'CODE_LOCATE', label: '代码定位 (CODE_LOCATE)' },
        { value: 'CODE_LOGIC', label: '代码逻辑 (CODE_LOGIC)' }
      ],
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
      authorityCodes: [],
      knowledgeCapability: {
        permissionDenied: false,
        fallbackActive: false,
        fallbackReason: ''
      },
      areaErrors: {
        message: '',
        messageType: 'error',
        session: '',
        knowledge: '',
        citations: '',
        debug: ''
	      },
	      lastRequest: null,
	      lastFailedRequest: null,
      messages: [],
      openContext: {
        sceneCode: '',
        actionCode: '',
        source: '',
        contextPayload: null
      },
      metricVersion: 0
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
    latestAssistantMessage() {
      return this.messages
        .slice()
        .reverse()
        .find(item => item.role === 'assistant' && (item.retrievalSummary || item.groundingStatus || (item.sources && item.sources.length))) || null
    },
    visibleEvidence() {
      const msg = this.latestAssistantMessage
      if (!msg) return null
      return msg.retrievalSummary || null
    },
    visibleEvidenceHits() {
      const evidence = this.visibleEvidence && this.visibleEvidence.evidence
      const hits = evidence && Array.isArray(evidence.hits) ? evidence.hits : []
      return hits.slice(0, 8)
    },
    visibleGroundingStatusText() {
      const msg = this.latestAssistantMessage
      const status = (msg && (msg.groundingStatus || (msg.retrievalSummary && msg.retrievalSummary.groundingStatus))) || 'NOT_CHECKED'
      return status
    },
    visibleStrictGroundingText() {
      const summary = this.visibleEvidence || {}
      return summary.strictGrounding ? 'ON' : 'OFF'
    },
    visibleDeclarationHitCount() {
      const summary = this.visibleEvidence || {}
      if (summary.declarationHitCount !== undefined && summary.declarationHitCount !== null) {
        return summary.declarationHitCount
      }
      const evidence = summary.evidence || {}
      return evidence.declarationHitCount || 0
    },
    visibleGraphExpandHitCount() {
      const summary = this.visibleEvidence || {}
      if (summary.graphExpandHitCount !== undefined && summary.graphExpandHitCount !== null) {
        return summary.graphExpandHitCount
      }
      const evidence = summary.evidence || {}
      return evidence.graphExpandHitCount || 0
    },
    visibleDegradeReason() {
      const summary = this.visibleEvidence || {}
      return summary.degradeReason || ''
    },
    visibleDegradedText() {
      const summary = this.visibleEvidence || {}
      return summary.degraded ? 'YES' : 'NO'
    },
    normalizedAuthorityCodes() {
      return (this.authorityCodes || [])
        .map(item => String(item || '').trim().toLowerCase())
        .filter(Boolean)
    },
    hasKnowledgeAuthority() {
      if (!this.isLoggedIn) return false
      if (!this.normalizedAuthorityCodes.length) return true
      if (this.hasDebugAuthority) return true
      return KB_AUTHORITY_HINTS.some(hint => this.normalizedAuthorityCodes.some(code => code.includes(String(hint).toLowerCase())))
    },
    hasDebugAuthority() {
      if (!this.isLoggedIn) return false
      if (!this.normalizedAuthorityCodes.length) return process.env.NODE_ENV !== 'production'
      return DEBUG_AUTHORITY_HINTS.some(hint => this.normalizedAuthorityCodes.some(code => code.includes(String(hint).toLowerCase())))
    },
    knowledgeFeatureEnabledByScene() {
      return this.sceneMeta.allowKnowledgeBinding !== false
    },
    canRequestKnowledgeBases() {
      return this.isLoggedIn && this.knowledgeFeatureEnabledByScene && this.hasKnowledgeAuthority && !this.knowledgeCapability.permissionDenied
    },
    canUseKnowledgeMode() {
      return this.knowledgeFeatureEnabledByScene && this.hasKnowledgeAuthority && !this.knowledgeCapability.permissionDenied
    },
    canSelectKnowledgeBase() {
      return this.canUseKnowledgeMode && this.knowledgeBaseOptions.length > 0
    },
    knowledgeSelectorDisabled() {
      return !this.canSelectKnowledgeBase || this.assistantMode !== ASSISTANT_MODE_KNOWLEDGE
    },
    knowledgePanelStatusText() {
      if (!this.knowledgeFeatureEnabledByScene) return '当前场景未启用知识库'
      if (this.knowledgeCapability.permissionDenied) return '当前账号无知识库权限，已降级为通用对话'
      if (!this.hasKnowledgeAuthority) return '权限策略未授权知识库，已禁用知识库问答'
      if (!this.knowledgeBaseOptions.length) return '暂无可用知识库'
      if (this.assistantMode !== ASSISTANT_MODE_KNOWLEDGE) return '当前为通用对话模式'
      return `已启用知识库问答（${this.selectedKnowledgeBaseIds.length || 0} 个）`
    },
    canToggleDeveloperMode() {
      return this.debugUiEnabled && this.hasDebugAuthority
    },
    debugUiEnabled() {
      if (process.env.NODE_ENV !== 'production') return true
      if (typeof window === 'undefined') return false
      try {
        const query = this.$route && this.$route.query ? this.$route.query : {}
        return window.localStorage.getItem(FORCE_DEBUG_STORAGE_KEY) === '1' || query.aiDebug === '1' || window.__AI_ASSISTANT_DEBUG__ === true
      } catch (e) {
        return false
      }
    },
    showDeveloperPanel() {
      return this.canToggleDeveloperMode && this.developerMode
    },
    sceneMeta() {
      const contextPayload =
        this.openContext && this.openContext.contextPayload && typeof this.openContext.contextPayload === 'object'
          ? this.openContext.contextPayload
          : {}
      return resolveAiSceneMeta({
        route: this.$route,
        sceneCode: this.openContext.sceneCode,
        currentKnowledgeBase: this.currentSceneKnowledgeBase,
        projectId: this.resolveContextProjectId(contextPayload)
      })
    },
    sceneLabel() {
      return this.sceneMeta.label
    },
    sceneSuggestions() {
      return getAiSuggestionPreset(this.sceneMeta.sceneCode).slice(0, 5)
    },
    sceneQuickPrompts() {
      const quickPrompts = Array.isArray(this.sceneMeta.defaultQuickPrompts) ? this.sceneMeta.defaultQuickPrompts : []
      return quickPrompts.length ? quickPrompts : this.sceneSuggestions
    },
    showSceneEmptyState() {
      return !this.sending && this.messages.length <= 1
    },
    contextPayloadSummary() {
      return summarizeAiContextPayload(this.openContext.contextPayload || {})
    },
    metricSnapshot() {
      void this.metricVersion
      return getAiUxMetricsSnapshot()
    },
    debugStateText() {
      return JSON.stringify({
        sceneCode: this.sceneMeta.sceneCode,
        actionCode: this.openContext.actionCode || '',
        assistantMode: this.assistantMode,
        analysisMode: this.analysisMode,
        contextPayloadSummary: this.contextPayloadSummary,
        sessionId: this.sessionId,
        selectedModelId: this.selectedModelId,
        selectedKnowledgeBaseIds: this.selectedKnowledgeBaseIds,
        canUseKnowledgeMode: this.canUseKnowledgeMode,
        knowledgeCapability: this.knowledgeCapability,
        activeStreamId: this.activeStream && this.activeStream.id,
        metrics: this.metricSnapshot,
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
	      this.clearOpenContext()
	      this.syncSceneKnowledgeBaseSelection()
	      this.syncAnalysisModeByScene()
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
    },
    canUseKnowledgeMode(val) {
      if (val) return
      this.applyKnowledgeModeFallback('capability-change')
    },
    canToggleDeveloperMode(val) {
      if (val) return
      this.developerMode = false
      this.persistDeveloperMode()
    },
	    strictGrounding(val) {
	      if (typeof window === 'undefined') return
	      localStorage.setItem(STRICT_GROUNDING_STORAGE_KEY, val ? '1' : '0')
    }
  },
  created() {
    this.messages = [this.createWelcomeMessage()]
  },
  mounted() {
    window.addEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.addEventListener('ai-assistant-open', this.handleAssistantOpenEvent)
    window.addEventListener('ai-ux-metric', this.handleUxMetricEvent)
    window.addEventListener('storage', this.handleStorageSync)
    window.addEventListener('mousemove', this.handleGlobalMouseMove)
	    window.addEventListener('mouseup', this.stopResize)
	    this.refreshAuthorityCodes()
	    this.restorePanelSize()
	    this.restoreDeveloperMode()
	    this.restoreAnalysisSettings()
      this.ensureAssistantModeByCapability()
	    this.syncSceneKnowledgeBaseSelection()
	  },
  beforeDestroy() {
    window.removeEventListener('ai-assistant-kb-change', this.handleSceneKnowledgeBaseChange)
    window.removeEventListener('ai-assistant-open', this.handleAssistantOpenEvent)
    window.removeEventListener('ai-ux-metric', this.handleUxMetricEvent)
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
        structured: null,
        insufficientContext: null,
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

    refreshAuthorityCodes() {
      if (typeof window === 'undefined') {
        this.authorityCodes = []
        return
      }
      const set = new Set()
      const storages = [window.localStorage, window.sessionStorage]
      storages.forEach(storage => {
        AUTH_STORAGE_KEYS.forEach(key => {
          try {
            const raw = storage.getItem(key)
            if (!raw) return
            const parsed = safeParsePermissionPayload(raw)
            if (parsed == null) appendPermissionCodes(set, raw)
            else appendPermissionCodes(set, parsed)
          } catch (e) {}
        })
      })
      if (this.$store && this.$store.state) appendPermissionCodes(set, this.$store.state)
      if (this.$store && this.$store.getters) appendPermissionCodes(set, this.$store.getters)
      this.authorityCodes = Array.from(set)
    },

    ensureAssistantModeByCapability() {
      if (!this.canUseKnowledgeMode) {
        this.applyKnowledgeModeFallback('capability-check')
      } else if (![ASSISTANT_MODE_GENERAL, ASSISTANT_MODE_KNOWLEDGE].includes(this.assistantMode)) {
        this.assistantMode = ASSISTANT_MODE_GENERAL
      }
    },

    applyKnowledgeModeFallback(reason = '') {
      this.assistantMode = ASSISTANT_MODE_GENERAL
      this.setKnowledgeBaseSelection([], { manual: false, source: reason || 'knowledge-fallback' })
      this.knowledgeCapability = {
        ...this.knowledgeCapability,
        fallbackActive: true,
        fallbackReason: reason || this.knowledgeCapability.fallbackReason || ''
      }
    },

    clearKnowledgeModeFallback() {
      this.knowledgeCapability = {
        ...this.knowledgeCapability,
        fallbackActive: false,
        fallbackReason: ''
      }
    },

    restoreDeveloperMode() {
      if (typeof window === 'undefined') return
      this.developerMode = this.canToggleDeveloperMode && localStorage.getItem(DEV_MODE_STORAGE_KEY) === '1'
    },

    persistDeveloperMode() {
      if (typeof window === 'undefined') return
      if (!this.canToggleDeveloperMode) {
        this.developerMode = false
        localStorage.removeItem(DEV_MODE_STORAGE_KEY)
        return
      }
      localStorage.setItem(DEV_MODE_STORAGE_KEY, this.developerMode ? '1' : '0')
    },

    handleAssistantModeChange(mode) {
      if (mode === ASSISTANT_MODE_KNOWLEDGE && !this.canUseKnowledgeMode) {
        this.applyKnowledgeModeFallback('mode-switch')
        return
      }
      if (mode === ASSISTANT_MODE_KNOWLEDGE && !this.canSelectKnowledgeBase) {
        this.applyKnowledgeModeFallback('empty-knowledge-base')
        this.setAreaError('knowledge', '暂无可用知识库，已切换为通用对话')
        return
      }
      if (mode !== ASSISTANT_MODE_KNOWLEDGE) {
        this.assistantMode = ASSISTANT_MODE_GENERAL
        return
      }
      this.assistantMode = ASSISTANT_MODE_KNOWLEDGE
      if (!this.selectedKnowledgeBaseIds.length && this.knowledgeBaseOptions.length) {
        this.setKnowledgeBaseSelection([this.knowledgeBaseOptions[0].id], { manual: false, source: 'mode-switch-default' })
      }
      this.clearAreaError('knowledge')
      this.clearKnowledgeModeFallback()
    },

    handleUxMetricEvent() {
      this.metricVersion += 1
    },

	    restoreAnalysisSettings() {
	      if (typeof window === 'undefined') return
	      const strict = localStorage.getItem(STRICT_GROUNDING_STORAGE_KEY)
	      if (strict === '1' || strict === '0') {
	        this.strictGrounding = strict === '1'
	      }
	      this.syncAnalysisModeByScene()
	    },

	    clearOpenContext() {
	      this.openContext = {
	        sceneCode: '',
	        actionCode: '',
	        source: '',
	        contextPayload: null
	      }
	    },

    getActiveSceneCode() {
      return (this.sceneMeta && this.sceneMeta.sceneCode) || 'global.assistant'
    },

    resolveContextProjectId(contextPayload = {}) {
      if (!contextPayload || typeof contextPayload !== 'object') return null
      const candidates = [
        contextPayload.projectId,
        contextPayload.project_id,
        contextPayload.project && contextPayload.project.id,
        contextPayload.project && contextPayload.project.projectId,
        contextPayload.projectBasicInfo && contextPayload.projectBasicInfo.id,
        contextPayload.enhancedContext &&
          contextPayload.enhancedContext.projectBasicInfo &&
          contextPayload.enhancedContext.projectBasicInfo.id
      ]
      for (const item of candidates) {
        const numeric = Number(item)
        if (Number.isFinite(numeric) && numeric > 0) return numeric
      }
      return null
    },

	    syncAnalysisModeByScene(options = {}) {
	      const { sceneCode = '', preferredMode = '' } = options
	      const targetSceneCode = sceneCode || this.getActiveSceneCode()
	      const defaultMode =
	        (this.sceneMeta && this.sceneMeta.sceneCode === targetSceneCode && this.sceneMeta.defaultAnalysisMode) ||
	        (this.analysisModeOptions[0] && this.analysisModeOptions[0].value) ||
	        'DOC_QA'

	      const mode =
	        (isValidAnalysisMode(preferredMode) && preferredMode) ||
	        resolveSceneAnalysisMode(targetSceneCode, defaultMode)

	      if (mode && this.analysisMode !== mode) {
	        this.analysisMode = mode
	      }
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

    looksLikeStructuredPayload(payload) {
      if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return false
      const contentKeys = ['summary', 'likelyQuestions', 'explanations', 'coverage', 'reason', 'nextAction']
      if (contentKeys.some(key => Object.prototype.hasOwnProperty.call(payload, key))) return true
      if (!Object.prototype.hasOwnProperty.call(payload, 'type')) return false
      const type = String(payload.type || '').trim().toLowerCase()
      return type === 'insufficient_context' || type === 'answer'
    },

    normalizeStructuredPayload(payload) {
      if (!this.looksLikeStructuredPayload(payload)) return null
      const normalized = { ...payload }
      if (normalized.type !== undefined && normalized.type !== null) {
        normalized.type = String(normalized.type).trim()
      }
      return normalized
    },

    getStructuredResponseType(structured) {
      if (!structured || typeof structured !== 'object') return ''
      return String(structured.type || '').trim().toLowerCase()
    },

    formatStructuredCoverage(coverage) {
      if (coverage === undefined || coverage === null) return ''
      if (typeof coverage === 'string') return coverage.trim()
      if (typeof coverage === 'number' || typeof coverage === 'boolean') return String(coverage)
      if (typeof coverage === 'object') {
        try {
          return JSON.stringify(coverage)
        } catch (e) {
          return ''
        }
      }
      return ''
    },

    normalizeStructuredList(value) {
      if (Array.isArray(value)) return value
      if (value === undefined || value === null || value === '') return []
      return [value]
    },

    normalizeStructuredListText(value) {
      if (typeof value === 'string') return value.trim()
      if (value && typeof value === 'object') {
        const candidates = [value.summary, value.title, value.text, value.content, value.label, value.question]
        for (const item of candidates) {
          if (item !== undefined && item !== null && String(item).trim()) return String(item).trim()
        }
      }
      if (value !== undefined && value !== null) return String(value).trim()
      return ''
    },

    resolveStructuredDisplayText(structured) {
      if (!structured || typeof structured !== 'object') return ''
      const type = this.getStructuredResponseType(structured)
      if (type === 'insufficient_context') {
        const lines = []
        if (structured.reason) lines.push(String(structured.reason).trim())
        const coverageText = this.formatStructuredCoverage(structured.coverage)
        if (coverageText) lines.push(`覆盖范围：${coverageText}`)
        if (structured.nextAction) lines.push(`建议动作：${String(structured.nextAction).trim()}`)
        return lines.filter(Boolean).join('\n')
      }

      const parts = []
      if (structured.summary) parts.push(String(structured.summary).trim())

      const explanations = this.normalizeStructuredList(structured.explanations)
        .map(this.normalizeStructuredListText)
        .filter(Boolean)
      if (explanations.length) {
        parts.push(explanations.map(item => `- ${item}`).join('\n'))
      }

      const likelyQuestions = this.normalizeStructuredList(structured.likelyQuestions)
        .map(this.normalizeStructuredListText)
        .filter(Boolean)
      if (likelyQuestions.length) {
        parts.push(`后续可问：\n${likelyQuestions.map(item => `- ${item}`).join('\n')}`)
      }

      return parts.filter(Boolean).join('\n\n').trim()
    },

    buildMessageInsufficientContext(structured) {
      if (this.getStructuredResponseType(structured) !== 'insufficient_context') return null
      return {
        type: 'insufficient_context',
        reason: String(structured.reason || '').trim(),
        coverage: structured.coverage !== undefined ? structured.coverage : '',
        nextAction: String(structured.nextAction || '').trim()
      }
    },

    applyStructuredMessageState(message, structured) {
      if (!message) return
      if (structured && typeof structured === 'object') {
        message.structured = structured
        message.insufficientContext = this.buildMessageInsufficientContext(structured)
        return
      }
      message.structured = null
      message.insufficientContext = null
    },

    extractStructuredPayloadFromResponse(rawResponse, displayText = '') {
      const candidates = []
      if (rawResponse && typeof rawResponse === 'object') {
        candidates.push(rawResponse.structured)
        candidates.push(rawResponse.result)
        candidates.push(rawResponse)
      }
      for (const candidate of candidates) {
        const normalized = this.normalizeStructuredPayload(candidate)
        if (normalized) return normalized
      }
      const text = String(displayText || '').trim()
      if (!text) return null
      let raw = text
      const fenced = raw.match(/```(?:json)?\s*([\s\S]*?)```/i)
      if (fenced && fenced[1]) raw = fenced[1].trim()
      try {
        const parsed = JSON.parse(raw)
        return this.normalizeStructuredPayload(parsed)
      } catch (e) {
        return null
      }
    },

    isInsufficientContextMessage(message) {
      if (!message || message.role !== 'assistant') return false
      if (message.insufficientContext && message.insufficientContext.type === 'insufficient_context') return true
      return this.getStructuredResponseType(message.structured) === 'insufficient_context'
    },

    emitInsufficientContextCta(action, message) {
      const detail = {
        ctaAction: String(action || '').trim(),
        type: 'insufficient_context',
        sceneCode: this.getActiveSceneCode(),
        actionCode: String((this.openContext && this.openContext.actionCode) || '').trim(),
        sessionId: this.sessionId || null,
        messageId: message && message.id ? message.id : '',
        insufficientContext:
          (message && message.insufficientContext) ||
          this.buildMessageInsufficientContext(message && message.structured) ||
          null
      }
      this.$emit('insufficient-context-cta', detail)
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('ai-assistant-insufficient-cta', { detail }))
      }
    },

    handleInsufficientContextCta(action, message) {
      const normalizedAction = String(action || '').trim()
      if (!normalizedAction) return
      recordAiUxMetric('insufficientContextCtaClick', {
        sceneCode: this.getActiveSceneCode(),
        actionCode: String((this.openContext && this.openContext.actionCode) || '').trim(),
        ctaAction: normalizedAction
      })
      this.emitInsufficientContextCta(normalizedAction, message)
      this.handleInsufficientContextAction(normalizedAction, message)
    },

    getLastUserQuestion(message = null) {
      const list = Array.isArray(this.messages) ? this.messages : []
      let endIndex = list.length - 1
      if (message && message.id) {
        const hitIndex = list.findIndex(item => item && item.id === message.id)
        if (hitIndex >= 0) endIndex = hitIndex - 1
      }
      for (let i = endIndex; i >= 0; i -= 1) {
        const item = list[i]
        if (item && item.role === 'user' && String(item.content || '').trim()) {
          return String(item.content || '').trim()
        }
      }
      return String((this.lastRequest && this.lastRequest.question) || '').trim()
    },

    refreshOpenContextFromCollector(reason = '') {
      if (!this.$aiActionBridge || typeof this.$aiActionBridge.collectContext !== 'function') return null
      const sceneCode = this.getActiveSceneCode()
      const collected = this.$aiActionBridge.collectContext(sceneCode, {
        reason,
        source: 'ai-assistant-insufficient-context'
      })
      if (!collected || typeof collected !== 'object' || Array.isArray(collected)) return null
      this.openContext = {
        ...this.openContext,
        contextPayload: {
          ...(this.openContext && this.openContext.contextPayload ? this.openContext.contextPayload : {}),
          ...collected
        }
      }
      return collected
    },

    sendInsufficientContextFollowup({ message = null, instruction = '', disableStrictGrounding = false } = {}) {
      if (this.sending) {
        this.$message.warning('AI 正在生成中，请稍后再试')
        return
      }
      const question = this.getLastUserQuestion(message)
      if (!question) {
        this.$message.warning('未找到上一轮问题，无法继续')
        return
      }
      if (disableStrictGrounding) {
        this.strictGrounding = false
      }
      this.input = [
        instruction || '请继续回答上一轮问题。',
        '',
        `上一轮问题：${question}`
      ].join('\n')
      this.$nextTick(() => this.send())
    },

    openKnowledgeBaseImportForCurrentContext() {
      const projectId = this.resolveContextProjectId(
        (this.openContext && this.openContext.contextPayload) || {}
      ) || (this.sceneMeta && this.sceneMeta.projectId) || null
      const query = {
        source: 'insufficient_context'
      }
      if (projectId) query.projectId = projectId
      this.visible = false
      this.$router.push({ path: '/knowledge-base', query })
    },

    handleInsufficientContextAction(action, message = null) {
      if (action === 'continue_current_page') {
        this.refreshOpenContextFromCollector('continue-current-page')
        this.sendInsufficientContextFollowup({
          message,
          disableStrictGrounding: true,
          instruction: '请仅基于当前页面已提供的上下文继续回答。不要编造未提供的信息；如果仍缺少信息，请明确列出缺口。'
        })
        return
      }

      if (action === 'read_more_project_docs') {
        const collected = this.refreshOpenContextFromCollector('read-more-project-docs')
        const enhanced = collected && (collected.enhancedContext || collected.readmeLeadText || collected.sources)
        if (!enhanced) {
          this.$message.info('已尝试刷新当前页面上下文；如果项目文档仍未加载，请先在项目页打开/同步文档后再试')
        }
        this.sendInsufficientContextFollowup({
          message,
          instruction: '请结合当前项目的 README、项目文档和关键文件上下文重新回答上一轮问题。若证据仍不足，请说明还需要导入哪些资料。'
        })
        return
      }

      if (action === 'import_knowledge_base') {
        this.openKnowledgeBaseImportForCurrentContext()
      }
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
      const retrievalSummary = raw.retrievalSummary || null
      const groundingStatus = raw.groundingStatus || (raw.grounding && raw.grounding.groundingStatus) || ''
      const sources = this.applyRetrievalReasonToSources(
        extractAiSources(raw, { callLogId }),
        retrievalSummary
      )
      const structured = this.extractStructuredPayloadFromResponse(raw, raw.content || raw.message || raw.answer || raw.text || '')
      const contentText = raw.content || raw.message || raw.answer || raw.text || this.resolveStructuredDisplayText(structured)
      return this.makeMessage(role, contentText, {
        id: raw.id || raw.messageId || `${role}-${Date.now()}-${Math.random()}`,
        sources,
        sourceOpen: false,
        callLogId,
        modelName: raw.modelName || '',
        retrievalSummary,
        groundingStatus,
        strictGrounding: raw.strictGrounding === true || (retrievalSummary && retrievalSummary.strictGrounding === true),
        structured,
        insufficientContext: this.buildMessageInsufficientContext(structured)
      })
    },

    applyRetrievalReasonToSources(sources = [], retrievalSummary = null) {
      if (!Array.isArray(sources) || !sources.length) return []
      const byChunkId =
        retrievalSummary &&
        retrievalSummary.evidence &&
        retrievalSummary.evidence.byChunkId &&
        typeof retrievalSummary.evidence.byChunkId === 'object'
          ? retrievalSummary.evidence.byChunkId
          : {}
      return sources.map(item => {
        const chunkKey = item && item.chunkId !== undefined && item.chunkId !== null ? String(item.chunkId) : ''
        const hit = chunkKey ? byChunkId[chunkKey] || null : null
        if (!hit) return item
        return {
          ...item,
          stageCode: item.stageCode || hit.stageCode || '',
          phase: item.phase || hit.phase || '',
          reason: item.reason || hit.reason || '',
          candidateSource: item.candidateSource || hit.candidateSource || '',
          symbolName: item.symbolName || hit.symbolName || '',
          symbolType: item.symbolType || hit.symbolType || '',
          path: item.path || hit.path || '',
          startLine: item.startLine !== null && item.startLine !== undefined ? item.startLine : hit.startLine,
          endLine: item.endLine !== null && item.endLine !== undefined ? item.endLine : hit.endLine
        }
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
      this.refreshAuthorityCodes()
      this.loadKnowledgeBases()
      this.ensureAssistantModeByCapability()
	      this.syncSceneKnowledgeBaseSelection()
	      this.syncSelectedModel()
	      this.syncAnalysisModeByScene()
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
      let list = (Array.isArray(ids) ? ids : (ids ? [ids] : [])).map(item => Number(item) || null).filter(Boolean)
      if (!this.canUseKnowledgeMode) list = []
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
      if (!this.canUseKnowledgeMode) {
        this.setKnowledgeBaseSelection([], { manual: false, source: 'capability-disabled' })
        return
      }
      const sceneKb = this.readCurrentKnowledgeBase()
      const path = (this.$route && this.$route.path) || ''
      const sceneCode = this.getActiveSceneCode()
      const isKnowledgeScene = path.includes('/knowledge-base') || sceneCode === 'knowledge.base'
      if (isKnowledgeScene && this.assistantMode !== ASSISTANT_MODE_KNOWLEDGE) {
        this.assistantMode = ASSISTANT_MODE_KNOWLEDGE
      }
      if (sceneKb && sceneKb.id) this.ensureKnowledgeBaseOption(sceneKb)
      if (isKnowledgeScene && sceneKb && sceneKb.id && !this.selectedKnowledgeBaseLocked) {
        this.setKnowledgeBaseSelection([sceneKb.id], { manual: false, source: 'scene-sync' })
        return
      }
      if (isKnowledgeScene && !this.selectedKnowledgeBaseIds.length) {
        this.setKnowledgeBaseSelection(this.readSelectedKnowledgeBaseIds(), { manual: true, source: 'remembered' })
        return
      }
      if (!isKnowledgeScene && !this.selectedKnowledgeBaseLocked && this.selectedKnowledgeBaseIds.length) {
        this.setKnowledgeBaseSelection([], { manual: false, source: 'non-knowledge-scene' })
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
      if (!this.canUseKnowledgeMode) return
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
      this.refreshAuthorityCodes()
	      await Promise.all([this.loadKnowledgeBases(), this.loadModels(), this.loadSessions()])
      this.ensureAssistantModeByCapability()
	      this.syncSceneKnowledgeBaseSelection()
	      this.syncAnalysisModeByScene()
	      this.$nextTick(this.scrollToBottom)
	    },

    openDrawer() {
      this.visible = true
    },

    async loadKnowledgeBases() {
      this.clearAreaError('knowledge')
      if (!this.userId) {
        this.knowledgeBaseOptions = []
        this.applyKnowledgeModeFallback('no-user')
        return
      }
      if (!this.knowledgeFeatureEnabledByScene) {
        this.knowledgeBaseOptions = []
        this.applyKnowledgeModeFallback('scene-disabled')
        return
      }
      if (!this.hasKnowledgeAuthority) {
        this.knowledgeBaseOptions = []
        this.knowledgeCapability = {
          ...this.knowledgeCapability,
          permissionDenied: true
        }
        this.applyKnowledgeModeFallback('authority-denied')
        this.setAreaError('knowledge', '当前账号未授权知识库能力，已切换为通用对话')
        return
      }
      try {
        const res = await pageMyKnowledgeBases({ page: 0, size: 80 }, { ownerId: this.userId })
        const pageData = this.extractPageData(res)
        this.knowledgeBaseOptions = (pageData.content || []).map(this.normalizeKnowledgeBase)
        const sceneKb = this.readCurrentKnowledgeBase()
        if (sceneKb && sceneKb.id) this.ensureKnowledgeBaseOption(sceneKb)
        if (this.assistantMode === ASSISTANT_MODE_KNOWLEDGE && !this.selectedKnowledgeBaseIds.length && this.knowledgeBaseOptions.length) {
          this.setKnowledgeBaseSelection([this.knowledgeBaseOptions[0].id], { manual: false, source: 'load-default' })
        }
        this.knowledgeCapability = {
          permissionDenied: false,
          fallbackActive: false,
          fallbackReason: ''
        }
        this.ensureAssistantModeByCapability()
      } catch (e) {
        const aiError = classifyAiError(e, '加载知识库失败')
        this.knowledgeBaseOptions = []
        if (aiError.type === 'forbidden' || Number(aiError.status || 0) === 403) {
          this.knowledgeCapability = {
            ...this.knowledgeCapability,
            permissionDenied: true
          }
          this.applyKnowledgeModeFallback('forbidden')
          this.setAreaError('knowledge', '当前账号没有知识库权限，已自动降级为通用对话')
          return
        }
        this.knowledgeCapability = {
          ...this.knowledgeCapability,
          permissionDenied: false
        }
        this.setAreaError('knowledge', e, '加载知识库失败')
        this.ensureAssistantModeByCapability()
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
        if (this.canUseKnowledgeMode) {
          this.sessions.forEach(item => (item.boundKnowledgeBases || []).forEach(this.ensureKnowledgeBaseOption))
        }
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
      if (this.canUseKnowledgeMode) {
        boundBases.forEach(this.ensureKnowledgeBaseOption)
      }
      const ids = []
      if (session.defaultKnowledgeBaseId) ids.push(session.defaultKnowledgeBaseId)
      if (session.recentKnowledgeBaseId && !ids.includes(session.recentKnowledgeBaseId)) ids.push(session.recentKnowledgeBaseId)
      ;(session.boundKnowledgeBaseIds || []).forEach(id => {
        if (id && !ids.includes(id)) ids.push(id)
      })
      if (this.canUseKnowledgeMode && ids.length) this.setKnowledgeBaseSelection(ids, { manual: false, source: 'session' })
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
      if (!this.canUseKnowledgeMode || this.assistantMode !== ASSISTANT_MODE_KNOWLEDGE) return
      this.setKnowledgeBaseSelection(val, { resetSession: true, manual: true, source: 'user-select' })
    },

    handleAnalysisModeChange(mode) {
      const normalized = String(mode || this.analysisMode || '').trim().toUpperCase()
      if (isValidAnalysisMode(normalized)) {
        saveSceneAnalysisMode(this.getActiveSceneCode(), normalized)
        recordAiUxMetric('manualModeSwitch', {
          sceneCode: this.getActiveSceneCode(),
          analysisMode: normalized
        })
      }
      if (this.sending) this.stopStream(false, 'analysis-mode-change')
    },

    handleStrictGroundingChange() {
      if (this.sending) this.stopStream(false, 'strict-grounding-change')
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

    tryParseStructuredResponse(rawResponse, displayText = '') {
      return this.extractStructuredPayloadFromResponse(rawResponse, displayText)
    },

    resolveApplyTargets(structured, rawResponse) {
      const candidates = [
        structured && structured.applyTargets,
        structured && structured.applyTarget,
        rawResponse && rawResponse.applyTargets,
        rawResponse && rawResponse.applyTarget
      ]
      for (const candidate of candidates) {
        if (Array.isArray(candidate)) return candidate
        if (candidate && typeof candidate === 'object') return [candidate]
      }
      return []
    },

    dispatchAssistantResultEvent({ sceneCode, actionCode = '', displayText = '', rawResponse = null }) {
      if (typeof window === 'undefined') return
      const structured = this.tryParseStructuredResponse(rawResponse, displayText)
      const applyTargets = this.resolveApplyTargets(structured, rawResponse)
      window.dispatchEvent(new CustomEvent('ai-assistant-result', {
        detail: {
          sceneCode: sceneCode || this.getActiveSceneCode(),
          actionCode: String(actionCode || '').trim(),
          structured,
          displayText: String(displayText || ''),
          rawResponse,
          applyTargets
        }
      }))
    },

    handleAssistantOpenEvent(event) {
      const detail = normalizeAiAssistantOpenPayload(event && event.detail)
      this.visible = true
      this.openContext = {
        sceneCode: detail.sceneCode || '',
        actionCode: detail.actionCode || '',
        source: detail.source || '',
        contextPayload: detail.contextPayload || null
      }
      this.syncAnalysisModeByScene({
        sceneCode: detail.sceneCode || '',
        preferredMode: detail.preferredAnalysisMode || ''
      })
      if (Array.isArray(detail.knowledgeBaseIds) && detail.knowledgeBaseIds.length && this.canUseKnowledgeMode) {
        this.setKnowledgeBaseSelection(detail.knowledgeBaseIds, { manual: true, source: detail.source || 'open-event' })
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
      const sceneMeta = this.sceneMeta || {}
      const kbIds = (Array.isArray(lockedKnowledgeBaseIds) ? lockedKnowledgeBaseIds : this.selectedKnowledgeBaseIds)
        .map(item => Number(item) || null)
        .filter(Boolean)
      const useKnowledgeMode = this.assistantMode === ASSISTANT_MODE_KNOWLEDGE && this.canUseKnowledgeMode
      const effectiveKnowledgeBaseIds = sceneMeta.allowKnowledgeBinding === false || !useKnowledgeMode ? [] : kbIds
      const contextPayload =
        this.openContext && this.openContext.contextPayload && typeof this.openContext.contextPayload === 'object'
          ? this.openContext.contextPayload
          : {}
      const actionCode =
        this.openContext && this.openContext.actionCode ? String(this.openContext.actionCode).trim() : ''
      const source = this.openContext && this.openContext.source ? String(this.openContext.source).trim() : ''
      const requestParams = {
        ...contextPayload,
        sceneCode: sceneMeta.sceneCode || 'global.assistant',
        source: source || 'ai-assistant'
      }
      if (actionCode) {
        requestParams.actionCode = actionCode
        requestParams.action_code = actionCode
      }
      if (this.analysisMode) {
        requestParams.analysisMode = this.analysisMode
        requestParams.analysis_mode = this.analysisMode
      }
      requestParams.assistantMode = this.assistantMode
      requestParams.assistant_mode = this.assistantMode
      requestParams.strictGrounding = this.strictGrounding
      requestParams.strict_grounding = this.strictGrounding
      return {
        sessionId: this.sessionId,
        userId: this.userId || undefined,
        content: question,
        modelId: this.selectedModelId || this.activeModelId || null,
        requestType: sceneMeta.requestType || 'CHAT',
        bizType: sceneMeta.bizType || 'GENERAL',
        projectId: sceneMeta.projectId || null,
        sceneCode: sceneMeta.sceneCode || 'global.assistant',
        sessionTitle: question.slice(0, 28) || 'AI 助手会话',
        memoryMode: 'SHORT',
        assistantMode: this.assistantMode,
        knowledgeBaseIds: effectiveKnowledgeBaseIds,
        defaultKnowledgeBaseId: effectiveKnowledgeBaseIds[0] || null,
        analysisMode: this.analysisMode,
        strictGrounding: this.strictGrounding,
        requestParams
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
      let finalRawResponse = null
      const resultSceneCode = payload.sceneCode || this.getActiveSceneCode()
      const resultActionCode = (this.openContext && this.openContext.actionCode) || ''
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
        const structured = this.extractStructuredPayloadFromResponse(finalRawResponse, partialText || assistantMessage.content || '')
        if (structured) this.applyStructuredMessageState(assistantMessage, structured)
        assistantMessage.status = '已完成'
        assistantMessage.content = partialText || assistantMessage.content || this.resolveStructuredDisplayText(assistantMessage.structured) || '已完成，但没有返回内容'
        this.dispatchAssistantResultEvent({
          sceneCode: resultSceneCode,
          actionCode: resultActionCode,
          displayText: assistantMessage.content,
          rawResponse:
            finalRawResponse ||
            {
              text: assistantMessage.content || '',
              sceneCode: resultSceneCode,
              sessionId: this.sessionId || payload.sessionId || null
            }
        })
        await this.ensureAssistantMessageSourceLoaded(assistantMessage)
        this.loadSessions()
        this.$nextTick(this.scrollToBottom)
      }

      const handleChunk = chunk => {
        if (!this.isActiveStream(streamId) || !chunk) return
        if (typeof chunk === 'object') {
          finalRawResponse = chunk
          if (chunk.sessionId) this.sessionId = chunk.sessionId
          if (chunk.modelId) this.activeModelId = chunk.modelId
          if (chunk.modelName) {
            this.activeModelName = chunk.modelName
            assistantMessage.modelName = chunk.modelName
          }
          const structured = this.extractStructuredPayloadFromResponse(chunk)
          if (structured) this.applyStructuredMessageState(assistantMessage, structured)
          if (chunk.callLogId) assistantMessage.callLogId = chunk.callLogId
          const sources = extractAiSources(chunk, { callLogId: chunk.callLogId || assistantMessage.callLogId, knowledgeBaseId: lockedKnowledgeBaseIds[0] || null })
          if (sources.length) {
            assistantMessage.sources = sources
            assistantMessage.sourceOpen = false
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
          const resolvedText = fullText || this.resolveStructuredDisplayText(assistantMessage.structured)
          if (resolvedText && (!partialText || resolvedText.length >= partialText.length)) {
            partialText = resolvedText
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
        finalRawResponse = data
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
          assistantMessage.sourceOpen = false
        }
        const structured = this.extractStructuredPayloadFromResponse(data)
        this.applyStructuredMessageState(assistantMessage, structured)
        partialText = extractAnswer(data) || this.resolveStructuredDisplayText(structured) || '已完成，但没有返回内容'
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
            assistantMessage.content = partialText || assistantMessage.content || '已保留当前内容'
            assistantMessage.status = '已完成（连接波动）'
            assistantMessage.errorType = ''
            assistantMessage.errorMessage = ''
            this.lastFailedRequest = { question, payload }
            this.setAreaError('message', `${aiError.label}：连接波动导致流式提前结束，已保留当前内容，可点击重试补全。`, '', 'warning')
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
      if (!this.showDeveloperPanel) return
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
  grid-template-columns: 220px minmax(0, 1fr) 320px;
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

.ai-insight {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.ai-insight .ai-card:last-child {
  margin-bottom: 0;
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

.knowledge-selected-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.ai-scene-empty {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px dashed var(--it-border);
  border-radius: 10px;
  background: var(--it-fill-soft);
  color: var(--it-text-secondary);
}

.ai-scene-empty__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--it-text-primary);
}

.ai-scene-empty__desc {
  margin-top: 6px;
  font-size: 12px;
}

.ai-scene-empty__list {
  margin: 10px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
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

.chat-item__insufficient {
  margin-top: 8px;
  padding: 10px;
  border: 1px dashed var(--it-border-strong, var(--it-border));
  border-radius: 8px;
  background: var(--it-fill-soft);
}

.chat-item__insufficient-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--it-text-subtle);
  line-height: 1.6;
}

.chat-item__insufficient-actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.global-ai-assistant ::v-deep .el-drawer__wrapper {
  background: transparent !important;
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
.ai-insight::-webkit-scrollbar,
.session-list::-webkit-scrollbar,
.chat-body::-webkit-scrollbar {
  width: 8px;
}

.ai-main::-webkit-scrollbar-thumb,
.ai-insight::-webkit-scrollbar-thumb,
.session-list::-webkit-scrollbar-thumb,
.chat-body::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.45);
  border-radius: 8px;
}

@media (max-width: 1200px) {
  .ai-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .ai-insight {
    grid-column: 2 / 3;
  }
}

@media (max-width: 900px) {
  .ai-workspace {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .ai-session-card {
    max-height: 180px;
  }

  .ai-main,
  .ai-insight {
    padding-right: 0;
    overflow: visible;
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
