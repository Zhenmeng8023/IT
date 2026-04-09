<template>
    <div class="notification-container">
      <!-- 消息铃铛按钮 -->
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
        <el-button 
          type="text" 
          class="notification-btn" 
          @click="openNotificationDrawer"
          icon="el-icon-bell"
        >
        </el-button>
      </el-badge>
  
      <!-- 消息侧边弹窗 -->
      <el-drawer
        title="消息通知"
        :visible.sync="drawerVisible"
        direction="rtl"
        size="400px"
        :before-close="handleDrawerClose"
        class="notification-drawer"
      >
        <div class="notification-list" v-loading="loading">
          <!-- 消息分类标签 -->
          <el-tabs v-model="activeTab" @tab-click="handleTabClick">
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="评论" name="comment"></el-tab-pane>
            <el-tab-pane label="点赞" name="like"></el-tab-pane>
            <el-tab-pane label="系统" name="system"></el-tab-pane>
          </el-tabs>
  
          <!-- 消息列表 -->
          <div class="notification-items">
            <div 
              v-for="notification in filteredNotifications" 
              :key="notification.id" 
              class="notification-item"
              :class="{ unread: !notification.readStatus }"
              @click="handleNotificationClick(notification)"
            >
              <!-- 头像 -->
              <el-avatar :size="40" :src="notification.senderAvatar || defaultAvatar" class="notification-avatar"></el-avatar>
              
              <!-- 消息内容 -->
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
  
              <!-- 未读红点 -->
              <div v-if="!notification.readStatus" class="unread-dot"></div>
            </div>
  
            <!-- 空状态 -->
            <div v-if="filteredNotifications.length === 0" class="empty-notification">
              <i class="el-icon-info"></i>
              <p>暂无消息</p>
            </div>
          </div>
  
          <!-- 加载更多 -->
          <div v-if="hasMore" class="load-more">
            <el-button type="text" @click="loadMore" :loading="loadingMore">加载更多</el-button>
          </div>
  
          <!-- 清空所有已读按钮 -->
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
        defaultAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
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

      async openNotificationDrawer() {
        if (!this.hasToken()) {
          this.$message.warning('请先登录')
          return
        }

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
  
  /* 消息分类标签 */
  .el-tabs {
    margin-bottom: 20px;
  }
  
  .el-tabs__item {
    font-size: 14px;
  }
  
  /* 消息列表区域 */
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
  
  /* 空状态 */
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
  
  /* 加载更多 */
  .load-more {
    text-align: center;
    padding: 15px 0;
  }
  
  /* 底部按钮 */
  .notification-footer {
    padding: 15px 0;
    text-align: center;
    border-top: 1px solid #f0f0f0;
  }
  </style>
