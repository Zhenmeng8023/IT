<template>
  <div class="notification-bell">
    <el-badge :value="badgeValue" :hidden="unreadCount <= 0" class="notification-badge">
      <el-button
        type="text"
        class="notification-button"
        icon="el-icon-bell"
        :disabled="!isLoggedIn"
        title="消息通知"
        @click="openDrawer"
      />
    </el-badge>

    <el-drawer
      title="消息通知"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="420px"
      class="notification-drawer"
      custom-class="notification-drawer-panel"
      modal-class="notification-modal-overlay"
      append-to-body
      modal-append-to-body
      :wrapper-closable="true"
      :destroy-on-close="true"
    >
      <div class="drawer-body">
        <div class="drawer-toolbar">
          <el-button type="text" size="small" :disabled="unreadCount <= 0" @click="markAllAsRead">
            全部已读
          </el-button>
          <el-button type="text" size="small" @click="goCenter">
            通知中心
          </el-button>
        </div>

        <el-tabs v-model="activeCategory" class="category-tabs" @tab-click="handleCategoryChange">
          <el-tab-pane v-for="item in categories" :key="item.value" :label="item.label" :name="item.value" />
        </el-tabs>

        <div v-loading="loading" class="notification-list">
          <button
            v-for="item in notifications"
            :key="item.id"
            type="button"
            class="notification-item"
            :class="{ unread: !item.readStatus }"
            @click="handleNotificationClick(item)"
          >
            <el-avatar :size="36" :src="item.senderAvatar || defaultAvatar" class="sender-avatar" />
            <span class="notification-main">
              <span class="notification-row">
                <span class="notification-title">{{ item.title || typeText(item) }}</span>
                <span class="notification-time">{{ formatFriendlyTime(item.createdAt) }}</span>
              </span>
              <span class="notification-content">{{ item.content || item.preview || '你收到一条新消息' }}</span>
              <span class="notification-meta">
                <span>{{ item.senderName || '系统' }}</span>
                <el-tag size="mini" effect="plain">{{ categoryText(item.category) }}</el-tag>
                <el-tag v-if="isProcessType(item)" size="mini" type="warning">去处理</el-tag>
              </span>
            </span>
            <span v-if="!item.readStatus" class="unread-dot"></span>
          </button>

          <el-empty v-if="!loading && !notifications.length" description="暂无通知" :image-size="96" />

          <div v-if="hasMore" class="load-more">
            <el-button type="text" :loading="loadingMore" @click="loadMore">加载更多</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead
} from '@/api/notification'
import { getToken } from '@/utils/auth'
import { navigateToNotificationTarget } from '@/utils/notificationNavigation'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

export default {
  name: 'NotificationBell',
  data() {
    return {
      drawerVisible: false,
      loading: false,
      loadingMore: false,
      unreadCount: 0,
      activeCategory: 'all',
      notifications: [],
      page: 1,
      size: 10,
      total: 0,
      timer: null,
      defaultAvatar: DEFAULT_AVATAR,
      categories: [
        { label: '全部', value: 'all' },
        { label: '互动', value: 'interaction' },
        { label: '项目', value: 'project' },
        { label: '邀请', value: 'invite' },
        { label: '请求', value: 'request' },
        { label: '系统', value: 'system' }
      ]
    }
  },
  computed: {
    isLoggedIn() {
      return !!getToken()
    },
    badgeValue() {
      return this.unreadCount > 99 ? '99+' : this.unreadCount
    },
    hasMore() {
      return this.notifications.length < this.total
    }
  },
  mounted() {
    if (this.isLoggedIn) {
      this.fetchUnreadCount()
      this.timer = window.setInterval(this.fetchUnreadCount, 30000)
    }
  },
  beforeDestroy() {
    this.clearTimer()
  },
  methods: {
    clearTimer() {
      if (this.timer) {
        window.clearInterval(this.timer)
        this.timer = null
      }
    },
    unwrap(response) {
      if (!response) return null
      if (response.data && response.data.data !== undefined) return response.data.data
      return response.data !== undefined ? response.data : response
    },
    buildParams() {
      const params = { page: this.page, size: this.size }
      if (this.activeCategory !== 'all') params.category = this.activeCategory
      return params
    },
    async fetchUnreadCount() {
      if (!this.isLoggedIn) {
        this.unreadCount = 0
        this.clearTimer()
        return
      }
      try {
        const data = this.unwrap(await getUnreadNotificationCount()) || {}
        this.unreadCount = Number(data.count || 0)
      } catch (error) {
        if (error && error.response && error.response.status !== 401) {
          console.error('刷新未读通知失败:', error)
        }
      }
    },
    async openDrawer() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录后查看通知')
        return
      }
      this.drawerVisible = true
      this.page = 1
      await this.fetchNotifications(true)
      await this.fetchUnreadCount()
    },
    async fetchNotifications(reset = false) {
      if (!this.isLoggedIn) {
        this.notifications = []
        this.total = 0
        return
      }
      if (reset) this.page = 1
      this.loading = reset
      this.loadingMore = !reset
      try {
        const data = this.unwrap(await getMyNotifications(this.buildParams())) || {}
        const list = Array.isArray(data.list) ? data.list : []
        this.total = Number(data.total || list.length || 0)
        this.notifications = reset ? list : this.notifications.concat(list)
      } catch (error) {
        console.error('加载通知失败:', error)
        if (!error || !error.response || error.response.status !== 401) {
          this.$message.error('通知加载失败，请稍后重试')
        }
      } finally {
        this.loading = false
        this.loadingMore = false
      }
    },
    async loadMore() {
      if (!this.hasMore || this.loadingMore) return
      this.page += 1
      await this.fetchNotifications(false)
    },
    handleCategoryChange() {
      this.page = 1
      this.fetchNotifications(true)
    },
    async handleNotificationClick(item) {
      if (!item) return
      if (!item.readStatus) {
        try {
          await markNotificationAsRead(item.id)
          item.readStatus = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (error) {
          console.error('标记通知已读失败:', error)
        }
      }
      this.drawerVisible = false
      await navigateToNotificationTarget(this, item)
    },
    async markAllAsRead() {
      if (!this.isLoggedIn) return
      try {
        const params = this.activeCategory === 'all' ? {} : { category: this.activeCategory }
        await markAllNotificationsAsRead(params)
        this.notifications = this.notifications.map(item => ({ ...item, readStatus: true }))
        await this.fetchUnreadCount()
        this.$message.success('已全部标记为已读')
      } catch (error) {
        console.error('全部标记已读失败:', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },
    goCenter() {
      this.drawerVisible = false
      this.$router.push('/notifications')
    },
    categoryText(category) {
      const matched = this.categories.find(item => item.value === category)
      return matched ? matched.label : '消息'
    },
    typeText(item) {
      const map = {
        comment: '新的评论',
        reply: '新的回复',
        like: '新的点赞',
        collect: '新的收藏',
        project_invitation: '项目邀请',
        project_join_request: '加入申请',
        project_star: '项目被收藏',
        system: '系统通知'
      }
      return map[item && item.type] || '消息通知'
    },
    isProcessType(item) {
      return item && ['project_invitation', 'project_join_request'].includes(item.type)
    },
    formatFriendlyTime(value) {
      if (!value) return ''
      const date = new Date(value)
      const diff = Math.floor((Date.now() - date.getTime()) / 1000)
      if (diff < 60) return '刚刚'
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
      if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${date.getFullYear()}-${month}-${day}`
    }
  }
}
</script>

<style scoped>
.notification-bell {
  display: inline-flex;
  align-items: center;
}

.notification-button {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 12px;
  color: var(--it-text-muted);
  font-size: 20px;
  background: var(--it-surface-solid);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow);
  transition: transform .2s ease, color .2s ease, border-color .2s ease, background-color .2s ease;
}

.notification-button:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
  transform: translateY(-1px);
}

.drawer-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 18px 18px;
  box-sizing: border-box;
  background: transparent;
}

.drawer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 2px;
}

.category-tabs {
  flex: 0 0 auto;
}

.notification-list {
  flex: 1;
  min-height: 240px;
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding-right: 4px;
}

.notification-item {
  display: flex;
  gap: 12px;
  position: relative;
  width: 100%;
  min-height: 86px;
  padding: 14px 18px 14px 12px;
  margin-bottom: 10px;
  border: 1px solid var(--it-border);
  border-radius: 16px;
  background: var(--it-surface-elevated, var(--it-surface-solid));
  cursor: pointer;
  text-align: left;
  box-shadow: var(--it-shadow);
  transition: transform .2s ease, background-color .2s ease, border-color .2s ease, box-shadow .2s ease;
}

.notification-item:hover {
  background-color: var(--it-surface-hover);
  border-color: var(--it-border-strong);
  transform: translateY(-1px);
}

.notification-item.unread {
  background: linear-gradient(135deg, var(--it-accent-soft), transparent 70%), var(--it-surface-elevated, var(--it-surface-solid));
}

.sender-avatar {
  flex: 0 0 auto;
}

.notification-main {
  min-width: 0;
  flex: 1;
  display: block;
}

.notification-row,
.notification-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--it-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.notification-content {
  display: -webkit-box;
  margin: 6px 0;
  color: var(--it-text-muted);
  font-size: 13px;
  line-height: 1.55;
  overflow: hidden;
  word-break: break-word;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-meta {
  justify-content: flex-start;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.unread-dot {
  position: absolute;
  top: 16px;
  right: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--it-danger, #f56c6c);
  box-shadow: 0 0 0 4px var(--it-surface-elevated, var(--it-surface-solid));
}

.load-more {
  text-align: center;
  padding: 8px 0 4px;
}

@media (max-width: 640px) {
  /deep/ .notification-drawer .el-drawer {
    width: 92vw !important;
  }

  .drawer-body {
    padding: 0 12px 14px;
  }
}
</style>