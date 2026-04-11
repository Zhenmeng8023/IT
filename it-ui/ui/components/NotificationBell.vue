<template>
  <div class="notification-container">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
      <el-button
        type="text"
        class="notification-btn"
        @click="openNotificationDrawer"
        icon="el-icon-bell"
      >
      </el-button>
    </el-badge>

    <el-drawer
      title="消息通知"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="400px"
      :before-close="handleDrawerClose"
      class="notification-drawer"
    >
      <div class="notification-list" v-loading="loading">
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <el-tab-pane label="全部" name="all"></el-tab-pane>
          <el-tab-pane label="评论" name="comment"></el-tab-pane>
          <el-tab-pane label="点赞" name="like"></el-tab-pane>
          <el-tab-pane label="私信" name="message"></el-tab-pane>
          <el-tab-pane label="系统" name="system"></el-tab-pane>
        </el-tabs>

        <div class="notification-items">
          <div
            v-for="notification in filteredNotifications"
            :key="notification.id"
            class="notification-item"
            :class="{ unread: !notification.readStatus }"
            @click="handleNotificationClick(notification)"
          >
            <el-avatar :size="40" :src="notification.senderAvatar || defaultAvatar" class="notification-avatar"></el-avatar>

            <div class="notification-content">
              <div class="notification-header">
                <span class="notification-type" :class="notification.type">
                  {{ getTypeText(notification.type) }}
                </span>
                <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
              </div>
              <div class="notification-body">
                <span class="notification-sender">{{ notification.senderName || '系统' }}</span>
                <span class="notification-action">{{ notification.actionText || '带来了新动态' }}</span>
                <span class="notification-target">{{ notification.targetTitle || '相关内容' }}</span>
              </div>
              <div class="notification-preview" v-if="notification.preview || notification.content">
                "{{ notification.preview || notification.content }}"
              </div>
            </div>

            <div v-if="!notification.readStatus" class="unread-dot"></div>
          </div>

          <div v-if="filteredNotifications.length === 0" class="empty-notification">
            <i class="el-icon-info"></i>
            <p>暂无消息</p>
          </div>
        </div>

        <div v-if="hasMore" class="load-more">
          <el-button type="text" @click="loadMore" :loading="loadingMore">加载更多</el-button>
        </div>

        <div class="notification-footer">
          <el-button type="text" @click="markAllAsRead" :disabled="unreadCount === 0">
            全部标为已读
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  GetMyNotifications,
  GetMyUnreadNotificationCount,
  MarkAllNotificationsAsRead,
  MarkNotificationAsRead
} from '@/api/index'
import { getToken } from '@/utils/auth'
import { subscribeRealtime } from '@/utils/realtimeClient'

export default {
  name: 'NotificationBell',
  data() {
    return {
      drawerVisible: false,
      activeTab: 'all',
      loading: false,
      loadingMore: false,
      hasMore: false,
      unreadCountValue: 0,
      notifications: [],
      defaultAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      realtimeUnsubscribers: []
    }
  },
  computed: {
    unreadCount() {
      return this.unreadCountValue
    },
    filteredNotifications() {
      if (this.activeTab === 'all') {
        return this.notifications
      }
      if (this.activeTab === 'comment') {
        return this.notifications.filter(n => ['comment', 'reply'].includes((n.type || '').toLowerCase()))
      }
      return this.notifications.filter(n => (n.type || '').toLowerCase() === this.activeTab)
    }
  },
  created() {
    if (process.client) {
      this.fetchUnreadCount()
    }
  },
  mounted() {
    this.bindRealtime()
  },
  beforeDestroy() {
    this.unbindRealtime()
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

    bindRealtime() {
      if (!process.client || !this.hasToken()) {
        return
      }
      this.unbindRealtime()
      this.realtimeUnsubscribers = [
        subscribeRealtime('notification-created', this.handleRealtimeCreated),
        subscribeRealtime('notification-read', this.handleRealtimeRead),
        subscribeRealtime('notification-read-all', this.handleRealtimeReadAll),
        subscribeRealtime('notification-deleted', this.handleRealtimeDeleted),
        subscribeRealtime('notification-cleared', this.handleRealtimeCleared)
      ]
    },

    unbindRealtime() {
      this.realtimeUnsubscribers.forEach(unsubscribe => unsubscribe && unsubscribe())
      this.realtimeUnsubscribers = []
    },

    async openNotificationDrawer() {
      if (!this.hasToken()) {
        this.$message.warning('请先登录')
        return
      }

      this.bindRealtime()
      this.drawerVisible = true
      await Promise.all([
        this.fetchNotifications(),
        this.fetchUnreadCount()
      ])
    },

    async fetchNotifications() {
      if (!this.hasToken()) {
        this.notifications = []
        this.unreadCountValue = 0
        return
      }

      this.loading = true
      try {
        const res = await GetMyNotifications()
        const data = this.unwrapResponse(res)
        this.notifications = Array.isArray(data) ? data : []
        this.unreadCountValue = this.notifications.filter(item => !item.readStatus).length
      } catch (error) {
        console.error('获取消息失败:', error)
        this.notifications = []
        if (error?.response?.status !== 401) {
          this.$message.error('获取消息失败')
        }
      } finally {
        this.loading = false
      }
    },

    async fetchUnreadCount() {
      if (!this.hasToken()) {
        this.unreadCountValue = 0
        return
      }

      try {
        const res = await GetMyUnreadNotificationCount()
        const data = this.unwrapResponse(res) || {}
        this.unreadCountValue = Number(data.count || 0)
      } catch (error) {
        if (error?.response?.status === 401) {
          this.unreadCountValue = 0
        }
      }
    },

    handleRealtimeCreated(payload) {
      const notification = payload?.notification
      if (!notification || !notification.id) {
        this.applyUnreadCount(payload?.unreadCount)
        return
      }

      const index = this.notifications.findIndex(item => Number(item.id) === Number(notification.id))
      if (index >= 0) {
        this.$set(this.notifications, index, notification)
      } else {
        this.notifications = [notification, ...this.notifications]
      }
      this.applyUnreadCount(payload?.unreadCount)
    },

    handleRealtimeRead(payload) {
      const notificationId = payload?.notificationId
      if (notificationId !== undefined && notificationId !== null) {
        const target = this.notifications.find(item => Number(item.id) === Number(notificationId))
        if (target) {
          this.$set(target, 'readStatus', true)
        }
      }
      this.applyUnreadCount(payload?.unreadCount)
    },

    handleRealtimeReadAll(payload) {
      this.notifications = this.notifications.map(item => ({
        ...item,
        readStatus: true
      }))
      this.applyUnreadCount(payload?.unreadCount ?? 0)
    },

    handleRealtimeDeleted(payload) {
      const notificationId = payload?.notificationId
      if (notificationId !== undefined && notificationId !== null) {
        this.notifications = this.notifications.filter(item => Number(item.id) !== Number(notificationId))
      }
      this.applyUnreadCount(payload?.unreadCount)
    },

    handleRealtimeCleared(payload) {
      this.notifications = []
      this.applyUnreadCount(payload?.unreadCount ?? 0)
    },

    applyUnreadCount(count) {
      if (count === undefined || count === null || Number.isNaN(Number(count))) {
        return
      }
      this.unreadCountValue = Number(count)
    },

    async loadMore() {
      this.loadingMore = false
    },

    async handleNotificationClick(notification) {
      await this.markAsRead(notification.id)
      this.drawerVisible = false

      if (notification.blogId) {
        const query = {}
        if (notification.commentId) {
          query.commentId = notification.commentId
          query.highlight = true
        }
        this.$router.push({
          path: `/blog/${notification.blogId}`,
          query
        })
        return
      }

      if ((notification.targetType || '').toLowerCase() === 'blog' && notification.targetId) {
        this.$router.push(`/blog/${notification.targetId}`)
        return
      }

      if ((notification.targetType || '').toLowerCase() === 'conversation' && notification.targetId) {
        this.$router.push({
          path: '/chat',
          query: {
            conversationId: notification.targetId
          }
        })
      }
    },

    async markAsRead(id) {
      const notification = this.notifications.find(n => n.id === id)
      if (!notification || notification.readStatus) {
        return
      }

      try {
        await MarkNotificationAsRead(id)
        notification.readStatus = true
        this.unreadCountValue = Math.max(0, this.unreadCountValue - 1)
      } catch (error) {
        console.error('标记通知已读失败:', error)
      }
    },

    async markAllAsRead() {
      if (!this.unreadCountValue) {
        return
      }

      try {
        await MarkAllNotificationsAsRead()
        this.notifications = this.notifications.map(item => ({
          ...item,
          readStatus: true
        }))
        this.unreadCountValue = 0
        this.$message.success('已全部标记为已读')
      } catch (error) {
        console.error('全部标记已读失败:', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },

    handleTabClick() {
      this.fetchNotifications()
    },

    handleDrawerClose(done) {
      done()
    },

    getTypeText(type) {
      const typeMap = {
        comment: '评论',
        reply: '回复',
        like: '点赞',
        message: '私信',
        system: '系统'
      }
      return typeMap[(type || '').toLowerCase()] || type
    },

    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = Math.floor((now - date) / 1000)

      if (diff < 60) return '刚刚'
      if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
      if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
      if (diff < 2592000) return Math.floor(diff / 86400) + '天前'

      const year = date.getFullYear()
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
}
</script>

<style scoped>
.notification-container {
  display: inline-block;
  margin-right: 15px;
}

.notification-badge {
  margin-right: 10px;
}

.notification-btn {
  font-size: 22px;
  padding: 8px;
  color: #606266;
  transition: color 0.2s;
}

.notification-btn:hover {
  color: #409EFF;
}

.notification-drawer .el-drawer__body {
  padding: 0;
}

.notification-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.el-tabs {
  margin-bottom: 20px;
}

.el-tabs__item {
  font-size: 14px;
}

.notification-items {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.notification-item.unread:hover {
  background-color: #e1f0ff;
}

.notification-avatar {
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.notification-type {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.notification-type.comment {
  background-color: #f0f9eb;
  color: #67c23a;
}

.notification-type.reply {
  background-color: #e6f7ff;
  color: #1890ff;
}

.notification-type.like {
  background-color: #fff7e6;
  color: #fa8c16;
}

.notification-type.message {
  background-color: #eef2ff;
  color: #4f46e5;
}

.notification-type.system {
  background-color: #f6f6f6;
  color: #666;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-body {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 5px;
}

.notification-sender {
  font-weight: 500;
  color: #409EFF;
  margin-right: 5px;
}

.notification-action {
  color: #606266;
  margin-right: 5px;
}

.notification-target {
  color: #303133;
  font-weight: 500;
}

.notification-preview {
  font-size: 13px;
  color: #909399;
  background-color: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
}

.empty-notification {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-notification i {
  font-size: 48px;
  margin-bottom: 10px;
}

.empty-notification p {
  font-size: 14px;
  margin: 0;
}

.load-more {
  text-align: center;
  padding: 15px 0;
}

.notification-footer {
  padding: 15px 0;
  text-align: center;
  border-top: 1px solid #f0f0f0;
}
</style>
