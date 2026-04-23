<template>
  <div>
    <div class="chat-config">
      <el-form :inline="true" size="small">
        <el-form-item label="当前用户">
          <el-input :value="chatForm.userId || '-'" disabled style="width: 120px" />
        </el-form-item>

        <el-form-item label="问答模型">
          <el-select
            :value="chatForm.modelId"
            clearable
            filterable
            placeholder="优先使用知识库默认模型"
            style="width: 240px"
            @input="updateChatField('modelId', $event)"
          >
            <el-option
              v-for="model in enabledModels"
              :key="model.id"
              :label="buildModelLabel(model)"
              :value="model.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button size="small" @click="$emit('use-kb-default-model')">使用知识库默认模型</el-button>
        </el-form-item>

        <el-form-item>
          <el-button size="small" @click="$emit('use-active-model')">使用系统当前模型</el-button>
        </el-form-item>

        <el-form-item>
          <el-button size="small" :loading="loadingModels" @click="$emit('refresh-models')">刷新模型</el-button>
        </el-form-item>

        <el-form-item label="请求类型">
          <el-select :value="chatForm.requestType" style="width: 160px" @input="updateChatField('requestType', $event)">
            <el-option label="KNOWLEDGE_QA" value="KNOWLEDGE_QA" />
            <el-option label="CHAT" value="CHAT" />
            <el-option label="SUMMARY" value="SUMMARY" />
            <el-option label="REWRITE" value="REWRITE" />
          </el-select>
        </el-form-item>

        <el-form-item label="记忆模式">
          <el-select :value="chatForm.memoryMode" style="width: 140px" @input="updateChatField('memoryMode', $event)">
            <el-option label="SHORT" value="SHORT" />
            <el-option label="SUMMARY" value="SUMMARY" />
            <el-option label="NONE" value="NONE" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button size="small" @click="$emit('new-session')">新建会话</el-button>
        </el-form-item>

        <el-form-item>
          <el-button size="small" @click="$emit('refresh-sessions')">刷新会话</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="chat-layout">
      <div class="chat-sidebar">
        <div class="chat-sidebar__toolbar">
          <el-input
            :value="sessionKeyword"
            size="small"
            clearable
            placeholder="搜索会话"
            prefix-icon="el-icon-search"
            @input="$emit('update:sessionKeyword', $event)"
          />
        </div>

        <div v-loading="sessionLoading" class="session-list">
          <el-empty
            v-if="!filteredSessions.length"
            description="暂无会话历史"
            :image-size="60"
          />

          <div
            v-for="item in filteredSessions"
            :key="item.id"
            class="session-item"
            :class="{ active: selectedSessionId === item.id }"
            @click="$emit('select-session', item)"
          >
            <div class="session-item__title">{{ item.title || `会话 #${item.id}` }}</div>
            <div class="session-item__meta">{{ formatTime(item.updatedAt || item.createdAt) }}</div>

            <div class="session-item__actions">
              <el-button type="text" size="mini" @click.stop="$emit('archive-session', item)">归档</el-button>
              <el-button type="text" size="mini" @click.stop="$emit('remove-session', item)">删除</el-button>
            </div>
          </div>
        </div>

        <div class="table-pagination session-pagination">
          <el-pagination
            background
            small
            layout="prev, pager, next"
            :current-page="sessionPagination.page + 1"
            :page-size="sessionPagination.size"
            :total="sessionPagination.total"
            @current-change="$emit('session-page-change', $event)"
          />
        </div>
      </div>

      <div class="chat-main">
        <div class="chat-box">
          <div ref="chatMessagesRef" class="chat-messages">
            <el-empty
              v-if="!chatMessages.length"
              description="先问一个问题，回答里会展示命中文档与切片来源"
              :image-size="80"
            />

            <div
              v-for="(msg, index) in chatMessages"
              :key="msg.id || `${msg.role}-${index}`"
              class="chat-message"
              :class="msg.role"
            >
              <div class="chat-message__role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
              <div class="chat-message__content">{{ msg.content }}</div>

              <div v-if="msg.role === 'assistant'" class="chat-message__footer">
                <span v-if="msg.totalTokens">Tokens {{ msg.totalTokens }}</span>
                <span v-if="msg.latencyMs">耗时 {{ msg.latencyMs }} ms</span>
                <span v-if="msg.modelName">模型 {{ msg.modelName }}</span>

                <el-button
                  v-if="showRetrievalActions && msg.callLogId"
                  type="text"
                  size="mini"
                  @click="$emit('open-retrieval-by-message', msg)"
                >
                  检索日志
                </el-button>
              </div>

              <div
                v-if="msg.role === 'assistant' && msg.sources && msg.sources.length"
                class="chat-message__sources"
              >
                <div class="sources-header" @click="$emit('toggle-message-sources', index)">
                  <span>命中文档 / 引用来源（{{ msg.sources.length }}）</span>
                  <i class="el-icon-arrow-down" :class="{ 'is-open': msg.sourceOpen }"></i>
                </div>

                <div v-show="msg.sourceOpen" class="sources-list">
                  <div
                    v-for="(source, sIndex) in msg.sources"
                    :key="source.id || `${index}-${sIndex}`"
                    class="source-card"
                  >
                    <div class="source-card__top">
                      <div class="source-card__title">{{ source.title || '未命中文档标题' }}</div>
                      <div class="source-card__meta">
                        <span v-if="source.score !== null && source.score !== undefined">Score {{ source.score }}</span>
                        <span v-if="source.chunkIndex !== null && source.chunkIndex !== undefined">Chunk #{{ source.chunkIndex }}</span>
                        <span v-if="source.retrievalMethod">{{ source.retrievalMethod }}</span>
                        <span v-if="source.keywordScore !== null && source.keywordScore !== undefined">关键词 {{ source.keywordScore }}</span>
                        <span v-if="source.vectorScore !== null && source.vectorScore !== undefined">向量 {{ source.vectorScore }}</span>
                      </div>
                    </div>

                    <div v-if="source.knowledgeBaseName" class="source-card__kb">
                      知识库：{{ source.knowledgeBaseName }}
                    </div>

                    <div v-if="source.archiveEntryPath || source.fileName" class="source-card__path">
                      {{ source.archiveEntryPath || source.fileName }}
                    </div>

                    <div class="source-card__actions">
                      <el-button type="text" size="mini" @click="$emit('locate-source-document', source)">定位文档</el-button>
                      <el-button
                        v-if="showRetrievalActions && source.callLogId"
                        type="text"
                        size="mini"
                        @click="$emit('open-retrieval', source.callLogId, source.title || '检索日志')"
                      >
                        检索日志
                      </el-button>
                    </div>

                    <el-collapse>
                      <el-collapse-item title="展开切片内容" :name="`${index}-${sIndex}`">
                        <div class="source-card__content">{{ source.content || '暂无切片内容' }}</div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <div class="chat-hint">
              当前默认绑定知识库：{{ currentKnowledgeBase.name }}
              <span>，知识库默认模型：{{ currentKnowledgeBaseDefaultModelName }}</span>
              <span>，当前生效模型：{{ effectiveChatModelName }}</span>
              <span v-if="selectedSessionId">，当前会话 ID：{{ selectedSessionId }}</span>
            </div>

            <el-input
              :value="chatForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入你的问题，例如：总结知识库里关于事务传播行为的要点并给出来源"
              @input="updateChatField('content', $event)"
              @keyup.ctrl.enter.native="$emit('send-chat', false)"
            />

            <div class="chat-actions">
              <el-button @click="$emit('clear-chat')">清空当前窗口</el-button>
              <el-button
                v-if="showDebugSearchAction"
                size="small"
                :loading="loadingDebugSearch"
                @click="$emit('debug-search')"
              >
                调试检索
              </el-button>
              <el-button :loading="loadingChat" @click="$emit('send-chat', false)">普通发送</el-button>
              <el-button
                v-if="loadingStreamChat"
                type="warning"
                @click="$emit('stop-stream')"
              >
                停止流式
              </el-button>
              <el-button
                v-else
                type="primary"
                :loading="loadingStreamChat"
                @click="$emit('send-chat', true)"
              >
                流式发送
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeBaseChatTab',
  props: {
    chatForm: {
      type: Object,
      default: () => ({})
    },
    enabledModels: {
      type: Array,
      default: () => []
    },
    loadingModels: {
      type: Boolean,
      default: false
    },
    sessionLoading: {
      type: Boolean,
      default: false
    },
    sessions: {
      type: Array,
      default: () => []
    },
    filteredSessions: {
      type: Array,
      default: () => []
    },
    sessionKeyword: {
      type: String,
      default: ''
    },
    sessionPagination: {
      type: Object,
      default: () => ({ page: 0, size: 10, total: 0 })
    },
    selectedSessionId: {
      type: [Number, String],
      default: null
    },
    chatMessages: {
      type: Array,
      default: () => []
    },
    currentKnowledgeBase: {
      type: Object,
      default: () => ({})
    },
    currentKnowledgeBaseDefaultModelName: {
      type: String,
      default: ''
    },
    effectiveChatModelName: {
      type: String,
      default: ''
    },
    loadingChat: {
      type: Boolean,
      default: false
    },
    loadingStreamChat: {
      type: Boolean,
      default: false
    },
    loadingDebugSearch: {
      type: Boolean,
      default: false
    },
    formatTime: {
      type: Function,
      default: value => value
    },
    buildModelLabel: {
      type: Function,
      default: model => (model && model.modelName) || ''
    },
    showDebugSearchAction: {
      type: Boolean,
      default: true
    },
    showRetrievalActions: {
      type: Boolean,
      default: true
    }
  },
  methods: {
    updateChatField(field, value) {
      this.$emit('update-chat-field', field, value)
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatMessagesRef
        if (el && typeof el.scrollTop === 'number') {
          el.scrollTop = el.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.chat-config {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.chat-layout {
  display: flex;
  gap: 16px;
  min-height: 560px;
}

.chat-sidebar {
  width: 280px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-sidebar__toolbar {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.session-list {
  flex: 1;
  overflow: auto;
  padding: 12px;
  min-height: 0;
}

.session-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover,
.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-item__title,
.source-card__title {
  font-weight: 600;
}

.session-item__meta,
.chat-hint,
.source-card__kb,
.source-card__meta {
  color: #909399;
  font-size: 12px;
}

.session-item__actions,
.source-card__actions,
.chat-message__footer,
.source-card__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.session-pagination {
  padding: 8px 12px 12px;
  border-top: 1px solid #ebeef5;
}

.chat-main {
  flex: 1;
  min-width: 0;
}

.chat-box {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
  background: #fafafa;
}

.chat-message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.chat-message.user {
  background: #f5f7fa;
}

.chat-message__role {
  font-weight: 600;
  margin-bottom: 8px;
}

.chat-message__content,
.source-card__content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.chat-message__footer {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.chat-message__sources {
  margin-top: 12px;
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}

.sources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-weight: 600;
}

.sources-header .is-open {
  transform: rotate(180deg);
}

.sources-list {
  margin-top: 10px;
}

.source-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fff;
}

.source-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.source-card__path {
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.chat-input {
  border-top: 1px solid #ebeef5;
  padding: 16px;
  background: #fff;
}

.chat-hint {
  margin-bottom: 10px;
}

.chat-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
