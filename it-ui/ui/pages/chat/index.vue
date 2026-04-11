<template>
  <div class="chat-page">
    <div class="chat-shell">
      <aside class="chat-sidebar">
        <div class="chat-sidebar__header">
          <div>
            <h2>消息中心</h2>
            <p>同一账号多端在线实时同步</p>
          </div>
          <el-button type="text" icon="el-icon-refresh" @click="loadConversations">刷新</el-button>
        </div>

        <div v-loading="conversationLoading" class="conversation-list">
          <div
            v-for="conversation in conversations"
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: Number(conversation.id) === Number(currentConversationId) }"
            @click="selectConversation(conversation.id)"
          >
            <div class="conversation-item__top">
              <span class="conversation-item__name">{{ conversation.name || '未命名会话' }}</span>
              <span class="conversation-item__time">{{ formatTime(conversation.lastMessageTime || conversation.updatedAt) }}</span>
            </div>
            <div class="conversation-item__bottom">
              <span class="conversation-item__preview">{{ conversation.lastMessageContent || '暂无消息' }}</span>
              <el-badge v-if="conversation.unreadCount" :value="conversation.unreadCount" class="conversation-item__badge"></el-badge>
            </div>
          </div>

          <div v-if="!conversationLoading && !conversations.length" class="chat-empty">
            暂无会话，可以从好友页发起私聊
          </div>
        </div>
      </aside>

      <section class="chat-main">
        <div class="chat-main__header">
          <div>
            <h3>{{ activeConversationName }}</h3>
            <p>{{ currentConversationDescription }}</p>
          </div>
        </div>

        <div ref="messageList" v-loading="messageLoading" class="message-list">
          <div v-if="!currentConversationId" class="chat-empty">
            从左侧选择会话开始聊天
          </div>

          <div v-else-if="!messages.length" class="chat-empty">
            还没有消息，发一条试试
          </div>

          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="{ self: Number(message.senderId) === Number(currentUserId) }"
          >
            <el-avatar
              :size="36"
              :src="message.senderAvatar || defaultAvatar"
              class="message-avatar"
            ></el-avatar>
            <div class="message-bubble">
              <div class="message-meta">
                <span class="message-author">{{ message.senderName || `用户 ${message.senderId}` }}</span>
                <span class="message-time">{{ formatDateTime(message.sentAt) }}</span>
              </div>
              <div class="message-content">{{ message.content }}</div>
            </div>
          </div>
        </div>

        <div class="chat-composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="3"
            resize="none"
            :disabled="!currentConversationId || sending"
            placeholder="输入消息，按 Ctrl + Enter 发送"
            @keyup.native.ctrl.enter="handleSend"
          ></el-input>
          <div class="chat-composer__actions">
            <span class="chat-composer__tip">Ctrl + Enter 发送</span>
            <el-button
              type="primary"
              :loading="sending"
              :disabled="!currentConversationId || !draft.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import {
  CreateConversation,
  GetConversationMessages,
  GetCurrentUser,
  GetMyConversations,
  MarkConversationMessagesAsRead,
  SendMessage
} from '@/api/index'
import { getToken } from '@/utils/auth'
import { subscribeRealtime } from '@/utils/realtimeClient'

export default {
  name: 'ChatPage',
  layout: 'blog',
  data() {
    return {
      conversations: [],
      messages: [],
      currentConversationId: null,
      currentConversation: null,
      currentUserId: null,
      conversationLoading: false,
      messageLoading: false,
      sending: false,
      draft: '',
      defaultAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      realtimeUnsubscribers: []
    }
  },
  computed: {
    activeConversationName() {
      return this.currentConversation?.name || '请选择会话'
    },
    currentConversationDescription() {
      if (!this.currentConversation) {
        return '支持同一账号多标签页、多窗口实时同步'
      }
      return `参与人数 ${this.currentConversation.participantCount || 0}，未读 ${this.currentConversation.unreadCount || 0}`
    }
  },
  async mounted() {
    if (!this.hasToken()) {
      this.$message.warning('请先登录后再使用聊天')
      return
    }

    await this.loadCurrentUser()
    await this.loadConversations()
    await this.bootstrapConversation()
    this.bindRealtime()
  },
  beforeDestroy() {
    this.unbindRealtime()
  },
  watch: {
    '$route.query': {
      async handler() {
        if (!this.currentUserId) return
        await this.bootstrapConversation()
      }
    }
  },
  methods: {
    unwrapResponse(res) {
      if (!res) return null
      if (res.data === undefined) return res
      const data = res.data
      if (
        data &&
        typeof data === 'object' &&
        Object.prototype.hasOwnProperty.call(data, 'data') &&
        (
          Object.prototype.hasOwnProperty.call(data, 'code') ||
          Object.prototype.hasOwnProperty.call(data, 'message') ||
          Object.prototype.hasOwnProperty.call(data, 'success')
        )
      ) {
        return data.data
      }
      return data
    },

    hasToken() {
      return !!getToken()
    },

    async loadCurrentUser() {
      const res = await GetCurrentUser()
      const data = this.unwrapResponse(res) || {}
      this.currentUserId = data.id || null
    },

    async loadConversations() {
      if (!this.hasToken()) return

      this.conversationLoading = true
      try {
        const res = await GetMyConversations()
        const data = this.unwrapResponse(res)
        this.conversations = Array.isArray(data) ? data : []
        if (this.currentConversationId) {
          this.currentConversation = this.conversations.find(item => Number(item.id) === Number(this.currentConversationId)) || null
        }
      } catch (error) {
        console.error('加载会话失败:', error)
        this.$message.error('加载会话失败')
      } finally {
        this.conversationLoading = false
      }
    },

    async bootstrapConversation() {
      const routeConversationId = Number(this.$route.query.conversationId || 0)
      const routeTargetUserId = Number(this.$route.query.targetUserId || 0)
      const routeTargetName = this.$route.query.targetName || ''

      if (routeConversationId) {
        if (Number(this.currentConversationId) !== routeConversationId) {
          await this.selectConversation(routeConversationId)
        }
        return
      }

      if (routeTargetUserId) {
        const conversation = await this.ensurePrivateConversation(routeTargetUserId, routeTargetName)
        if (conversation?.id) {
          await this.$router.replace({
            path: '/chat',
            query: {
              conversationId: conversation.id
            }
          })
        }
        return
      }

      if (!this.currentConversationId && this.conversations.length) {
        await this.selectConversation(this.conversations[0].id)
      }
    },

    async ensurePrivateConversation(targetUserId, targetName = '') {
      if (!targetUserId || Number(targetUserId) === Number(this.currentUserId)) {
        return null
      }

      const existing = this.conversations.find(item => {
        const ids = Array.isArray(item.participantIds) ? item.participantIds.map(Number) : []
        return item.type === 'private' &&
          ids.length === 2 &&
          ids.includes(Number(this.currentUserId)) &&
          ids.includes(Number(targetUserId))
      })
      if (existing) {
        return existing
      }

      try {
        const res = await CreateConversation({
          type: 'private',
          name: targetName,
          participantIds: [targetUserId]
        })
        const conversation = this.unwrapResponse(res)
        await this.loadConversations()
        return conversation
      } catch (error) {
        console.error('创建私聊失败:', error)
        this.$message.error('创建私聊失败')
        return null
      }
    },

    async selectConversation(conversationId) {
      if (!conversationId) return
      this.currentConversationId = Number(conversationId)
      this.currentConversation = this.conversations.find(item => Number(item.id) === Number(conversationId)) || null
      await this.loadMessages(conversationId)
      await this.markConversationRead(conversationId)
    },

    async loadMessages(conversationId) {
      this.messageLoading = true
      try {
        const res = await GetConversationMessages(conversationId)
        const data = this.unwrapResponse(res)
        this.messages = Array.isArray(data) ? data : []
        this.$nextTick(this.scrollToBottom)
      } catch (error) {
        console.error('加载消息失败:', error)
        this.$message.error('加载消息失败')
      } finally {
        this.messageLoading = false
      }
    },

    async markConversationRead(conversationId) {
      try {
        await MarkConversationMessagesAsRead(conversationId)
        const conversation = this.conversations.find(item => Number(item.id) === Number(conversationId))
        if (conversation) {
          this.$set(conversation, 'unreadCount', 0)
        }
        if (this.currentConversation && Number(this.currentConversation.id) === Number(conversationId)) {
          this.$set(this.currentConversation, 'unreadCount', 0)
        }
      } catch (error) {
        console.error('标记已读失败:', error)
      }
    },

    async handleSend() {
      if (!this.currentConversationId || !this.draft.trim()) {
        return
      }

      const content = this.draft.trim()
      this.sending = true
      try {
        const res = await SendMessage({
          conversationId: this.currentConversationId,
          content
        })
        const message = this.unwrapResponse(res)
        this.mergeIncomingMessage(message)
        this.draft = ''
        await this.loadConversations()
      } catch (error) {
        console.error('发送消息失败:', error)
        this.$message.error('发送消息失败')
      } finally {
        this.sending = false
      }
    },

    bindRealtime() {
      this.unbindRealtime()
      this.realtimeUnsubscribers = [
        subscribeRealtime('message-created', this.handleRealtimeMessageCreated),
        subscribeRealtime('conversation-updated', this.handleRealtimeConversationUpdated),
        subscribeRealtime('conversation-read', this.handleRealtimeConversationRead)
      ]
    },

    unbindRealtime() {
      this.realtimeUnsubscribers.forEach(unsubscribe => unsubscribe && unsubscribe())
      this.realtimeUnsubscribers = []
    },

    handleRealtimeMessageCreated(message) {
      if (!message || !message.conversationId) {
        return
      }

      if (Number(message.conversationId) === Number(this.currentConversationId)) {
        this.mergeIncomingMessage(message)
        if (Number(message.senderId) !== Number(this.currentUserId)) {
          this.markConversationRead(this.currentConversationId)
        }
      } else {
        this.loadConversations()
      }
    },

    handleRealtimeConversationUpdated(payload) {
      if (!payload || !payload.conversationId) {
        return
      }

      const conversation = this.conversations.find(item => Number(item.id) === Number(payload.conversationId))
      if (!conversation) {
        this.loadConversations()
        return
      }

      if (payload.lastMessageContent !== undefined) {
        this.$set(conversation, 'lastMessageContent', payload.lastMessageContent)
      }
      if (payload.lastMessageTime !== undefined) {
        this.$set(conversation, 'lastMessageTime', payload.lastMessageTime)
      }
      if (payload.unreadCount !== undefined) {
        this.$set(conversation, 'unreadCount', Number(payload.unreadCount || 0))
      }
      this.moveConversationToTop(conversation.id)
      if (this.currentConversation && Number(this.currentConversation.id) === Number(conversation.id)) {
        this.currentConversation = conversation
      }
    },

    handleRealtimeConversationRead(payload) {
      if (!payload || !payload.conversationId) {
        return
      }
      const conversation = this.conversations.find(item => Number(item.id) === Number(payload.conversationId))
      if (conversation && payload.unreadCount !== undefined) {
        this.$set(conversation, 'unreadCount', Number(payload.unreadCount || 0))
      }
    },

    mergeIncomingMessage(message) {
      if (!message || !message.id) {
        return
      }
      const exists = this.messages.some(item => Number(item.id) === Number(message.id))
      if (exists) {
        return
      }
      this.messages = [...this.messages, message].sort((left, right) => {
        const leftTime = new Date(left.sentAt || 0).getTime()
        const rightTime = new Date(right.sentAt || 0).getTime()
        if (leftTime === rightTime) {
          return Number(left.id || 0) - Number(right.id || 0)
        }
        return leftTime - rightTime
      })
      this.$nextTick(this.scrollToBottom)
    },

    moveConversationToTop(conversationId) {
      const index = this.conversations.findIndex(item => Number(item.id) === Number(conversationId))
      if (index <= 0) {
        return
      }
      const [conversation] = this.conversations.splice(index, 1)
      this.conversations = [conversation, ...this.conversations]
    },

    scrollToBottom() {
      const container = this.$refs.messageList
      if (!container) return
      container.scrollTop = container.scrollHeight
    },

    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = Math.floor((now - date) / 1000)

      if (diff < 60) return '刚刚'
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`

      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      return `${month}-${day}`
    },

    formatDateTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      const hour = `${date.getHours()}`.padStart(2, '0')
      const minute = `${date.getMinutes()}`.padStart(2, '0')
      return `${month}-${day} ${hour}:${minute}`
    }
  }
}
</script>

<style scoped>
.chat-page {
  min-height: calc(100vh - 110px);
}

.chat-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 140px);
}

.chat-sidebar,
.chat-main {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.08);
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-sidebar__header,
.chat-main__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #eef2f7;
}

.chat-sidebar__header h2,
.chat-main__header h3 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.chat-sidebar__header p,
.chat-main__header p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.conversation-item {
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 10px;
  background: #f8fafc;
  border: 1px solid transparent;
}

.conversation-item:hover {
  transform: translateY(-1px);
  border-color: #cbd5e1;
}

.conversation-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%);
  border-color: #93c5fd;
}

.conversation-item__top,
.conversation-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.conversation-item__name {
  font-weight: 600;
  color: #1f2937;
}

.conversation-item__time {
  color: #94a3b8;
  font-size: 12px;
  flex-shrink: 0;
}

.conversation-item__preview {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item__badge {
  margin-left: 10px;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background:
    radial-gradient(circle at top right, rgba(191, 219, 254, 0.3), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f5f7fb 100%);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.message-row.self {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: min(72%, 680px);
  background: #fff;
  border-radius: 16px;
  padding: 12px 14px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.07);
}

.message-row.self .message-bubble {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
  color: #fff;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.message-row.self .message-meta {
  color: rgba(255, 255, 255, 0.75);
}

.message-author {
  font-weight: 600;
}

.message-content {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-composer {
  padding: 18px 20px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.chat-composer__actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-composer__tip {
  color: #94a3b8;
  font-size: 12px;
}

.chat-empty {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  text-align: center;
  padding: 20px;
}

@media (max-width: 960px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    min-height: 260px;
  }

  .message-bubble {
    max-width: 88%;
  }
}
</style>
