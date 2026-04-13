<template>
  <div class="notification-page">
    <div class="page-header">
      <div>
        <h1>通知中心</h1>
        <p>查看互动、项目、邀请、请求和系统消息</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!unreadTotal" @click="markAllAsRead">全部已读</el-button>
        <el-button type="danger" plain :disabled="!notifications.length" @click="clearAll">清空通知</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="activeCategory" size="small" @change="handleFilterChange">
        <el-radio-button v-for="item in categories" :key="item.value" :label="item.value">
          {{ item.label }}<span v-if="unreadCounts[item.value]" class="count-text">{{ unreadCounts[item.value] }}</span>
        </el-radio-button>
      </el-radio-group>

      <el-radio-group v-model="readFilter" size="small" @change="handleFilterChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="false">未读</el-radio-button>
        <el-radio-button label="true">已读</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="notification-list">
      <div v-for="item in notifications" :key="item.id" class="notification-card" :class="{ unread: !item.readStatus }">
        <div v-if="!item.readStatus" class="card-dot"></div>
        <div class="card-main" @click="openNotification(item)">
          <div class="card-title-row">
            <h3>{{ item.title || '消息通知' }}</h3>
            <span>{{ formatFriendlyTime(item.createdAt) }}</span>
          </div>
          <p>{{ item.content || item.preview || '你收到一条新消息' }}</p>
          <div class="card-meta">
            <span>发送人：{{ item.senderName || '系统' }}</span>
            <el-tag size="mini" effect="plain">{{ categoryText(item.category) }}</el-tag>
            <el-tag v-if="item.type" size="mini" :type="tagType(item.category)">{{ typeText(item.type) }}</el-tag>
            <span v-if="item.targetTitle">目标：{{ item.targetTitle }}</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button v-if="isProcessType(item)" type="primary" size="mini" @click="openNotification(item)">去处理</el-button>
          <el-button size="mini" :disabled="item.readStatus" @click="markRead(item)">标为已读</el-button>
          <el-button size="mini" type="danger" plain @click="removeItem(item)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && !notifications.length" description="暂无通知" />
    </div>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :current-page.sync="page"
        :page-size="size"
        :total="total"
        @current-change="fetchNotifications"
      />
    </div>
  </div>
</template>

<script>
import {
  clearNotifications,
  deleteNotification,
  getMyNotifications,
  getUnreadNotificationCounts,
  markAllNotificationsAsRead,
  markNotificationAsRead
} from '@/api/notification'
import { getToken } from '@/utils/auth'
import { navigateToNotificationTarget } from '@/utils/notificationNavigation'

export default {
  name: 'NotificationCenterPage',
  layout: 'default',
  data() {
    return {
      loading: false,
      notifications: [],
      page: 1,
      size: 12,
      total: 0,
      activeCategory: 'all',
      readFilter: '',
      unreadCounts: {},
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
    unreadTotal() {
      return Number(this.unreadCounts.all || 0)
    }
  },
  mounted() {
    if (!getToken()) {
      this.$message.warning('请先登录')
      this.$router.replace('/login')
      return
    }
    this.fetchUnreadCounts()
    this.fetchNotifications()
  },
  methods: {
    unwrap(response) {
      if (!response) return null
      if (response.data && response.data.data !== undefined) return response.data.data
      return response.data !== undefined ? response.data : response
    },
    buildParams() {
      const params = { page: this.page, size: this.size }
      if (this.activeCategory !== 'all') params.category = this.activeCategory
      if (this.readFilter !== '') params.readStatus = this.readFilter
      return params
    },
    async fetchNotifications() {
      this.loading = true
      try {
        const data = this.unwrap(await getMyNotifications(this.buildParams())) || {}
        this.notifications = Array.isArray(data.list) ? data.list : []
        this.total = Number(data.total || 0)
      } catch (error) {
        console.error('加载通知列表失败:', error)
        this.$message.error('通知列表加载失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    async fetchUnreadCounts() {
      try {
        this.unreadCounts = this.unwrap(await getUnreadNotificationCounts()) || {}
      } catch (error) {
        console.error('加载未读统计失败:', error)
        this.unreadCounts = {}
      }
    },
    handleFilterChange() {
      this.page = 1
      this.fetchNotifications()
    },
    async markRead(item) {
      if (!item || item.readStatus) return
      try {
        await markNotificationAsRead(item.id)
        item.readStatus = true
        await this.fetchUnreadCounts()
      } catch (error) {
        console.error('标记已读失败:', error)
        this.$message.error('标记已读失败，请稍后重试')
      }
    },
    async openNotification(item) {
      await this.markRead(item)
      await navigateToNotificationTarget(this, item)
    },
    async markAllAsRead() {
      try {
        const params = this.activeCategory === 'all' ? {} : { category: this.activeCategory }
        await markAllNotificationsAsRead(params)
        await Promise.all([this.fetchUnreadCounts(), this.fetchNotifications()])
        this.$message.success('已全部标记为已读')
      } catch (error) {
        console.error('全部标记已读失败:', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },
    async removeItem(item) {
      try {
        await this.$confirm('确定删除这条通知吗？', '删除通知', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteNotification(item.id)
        await Promise.all([this.fetchUnreadCounts(), this.fetchNotifications()])
        this.$message.success('通知已删除')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除通知失败:', error)
          this.$message.error('删除通知失败，请稍后重试')
        }
      }
    },
    async clearAll() {
      try {
        await this.$confirm('确定清空所有通知吗？该操作只会清空你的通知。', '清空通知', {
          confirmButtonText: '清空',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await clearNotifications()
        await Promise.all([this.fetchUnreadCounts(), this.fetchNotifications()])
        this.$message.success('通知已清空')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清空通知失败:', error)
          this.$message.error('清空通知失败，请稍后重试')
        }
      }
    },
    isProcessType(item) {
      return item && ['project_invitation', 'project_join_request'].includes(item.type)
    },
    categoryText(category) {
      const matched = this.categories.find(item => item.value === category)
      return matched ? matched.label : '消息'
    },
    tagType(category) {
      return { invite: 'warning', request: 'danger', system: 'info', interaction: 'success' }[category] || 'primary'
    },
    typeText(type) {
      const map = {
        comment: '评论',
        reply: '回复',
        like: '点赞',
        collect: '收藏',
        project_invitation: '项目邀请',
        project_join_request: '加入申请',
        project_star: '项目收藏',
        system: '系统'
      }
      return map[type] || type
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
.notification-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
}

.page-header,
.filter-bar,
.notification-card,
.card-title-row,
.card-meta,
.card-actions {
  display: flex;
  align-items: center;
}

.page-header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
  font-size: 26px;
}

.page-header p {
  margin: 8px 0 0;
  color: #909399;
}

.header-actions,
.filter-bar {
  gap: 10px;
  flex-wrap: wrap;
}

.filter-bar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.count-text {
  margin-left: 4px;
  color: #f56c6c;
}

.notification-list {
  min-height: 360px;
}

.notification-card {
  position: relative;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  min-height: 108px;
  padding: 18px 18px 18px 22px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.notification-card:hover {
  border-color: #c6e2ff;
}

.notification-card.unread {
  border-color: #b3d8ff;
  background: #f5faff;
}

.card-dot {
  position: absolute;
  left: 8px;
  top: 24px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.card-main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.card-title-row {
  justify-content: space-between;
  gap: 12px;
}

.card-title-row h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title-row span,
.card-meta {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.card-main p {
  display: -webkit-box;
  margin: 8px 0;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  word-break: break-word;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  gap: 10px;
  flex-wrap: wrap;
}

.card-actions {
  flex: 0 0 220px;
  justify-content: flex-end;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .notification-page {
    padding: 16px;
  }

  .page-header,
  .notification-card {
    flex-direction: column;
    align-items: stretch;
  }

  .card-actions {
    flex: 0 0 auto;
    justify-content: flex-end;
  }
}
</style>
