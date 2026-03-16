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
            <el-tab-pane label="回复" name="reply"></el-tab-pane>
            <el-tab-pane label="点赞" name="like"></el-tab-pane>
            <el-tab-pane label="系统" name="system"></el-tab-pane>
          </el-tabs>
  
          <!-- 消息列表 -->
          <div class="notification-items">
            <div 
              v-for="notification in filteredNotifications" 
              :key="notification.id" 
              class="notification-item"
              :class="{ 'unread': !notification.isRead }"
              @click="handleNotificationClick(notification)"
            >
              <!-- 头像 -->
              <el-avatar :size="40" :src="notification.avatar" class="notification-avatar"></el-avatar>
              
              <!-- 消息内容 -->
              <div class="notification-content">
                <div class="notification-header">
                  <span class="notification-type" :class="notification.type">
                    {{ getTypeText(notification.type) }}
                  </span>
                  <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
                </div>
                <div class="notification-body">
                  <span class="notification-sender">{{ notification.sender }}</span>
                  <span class="notification-action">{{ notification.action }}</span>
                  <span class="notification-target">{{ notification.target }}</span>
                </div>
                <div class="notification-preview" v-if="notification.preview">
                  "{{ notification.preview }}"
                </div>
              </div>
  
              <!-- 未读红点 -->
              <div v-if="!notification.isRead" class="unread-dot"></div>
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
  export default {
    name: 'NotificationBell',
    data() {
      return {
        drawerVisible: false,
        activeTab: 'all',
        loading: false,
        loadingMore: false,
        page: 1,
        pageSize: 20,
        hasMore: true,
        notifications: [
          // 模拟消息数据
          {
            id: 1,
            type: 'reply', // reply, like, system
            avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
            sender: '前端小迷妹',
            action: '回复了你的评论',
            target: 'Vue 2 组合式 API 实践与思考',
            targetId: 123, // 博客ID
            commentId: 456, // 评论ID
            preview: '写的真好，受益匪浅！',
            createTime: '2025-03-16T10:30:00Z',
            isRead: false,
          },
          {
            id: 2,
            type: 'reply',
            avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
            sender: '代码猎人',
            action: '回复了你的评论',
            target: 'Java 并发编程实战',
            targetId: 456,
            commentId: 789,
            preview: '这个问题我也遇到过...',
            createTime: '2025-03-16T09:15:00Z',
            isRead: true,
          },
          {
            id: 3,
            type: 'like',
            avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            sender: '技术小白',
            action: '点赞了你的博客',
            target: 'Docker 容器化部署',
            targetId: 789,
            createTime: '2025-03-15T22:30:00Z',
            isRead: false,
          },
          {
            id: 4,
            type: 'system',
            avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
            sender: '系统',
            action: '你的博客已通过审核',
            target: 'Vue 3 入门指南',
            targetId: 101,
            createTime: '2025-03-15T14:20:00Z',
            isRead: false,
          },
          {
            id: 5,
            type: 'reply',
            avatar: 'https://cube.elemecdn.com/2/8e/4a7b8c9d0e1f2a3b4c5d6e7f8g9h0i.png',
            sender: '算法爱好者',
            action: '在评论中提到了你',
            target: '算法刷题心得',
            targetId: 202,
            commentId: 303,
            preview: '@当前用户 你觉得这个解法怎么样？',
            createTime: '2025-03-14T16:45:00Z',
            isRead: true,
          },
        ],
      };
    },
    computed: {
      // 未读消息数量
      unreadCount() {
        return this.notifications.filter(n => !n.isRead).length;
      },
      // 根据当前标签过滤消息
      filteredNotifications() {
        if (this.activeTab === 'all') {
          return this.notifications;
        }
        return this.notifications.filter(n => n.type === this.activeTab);
      },
    },
    methods: {
      // 打开消息抽屉
      openNotificationDrawer() {
        this.drawerVisible = true;
        this.fetchNotifications();
      },
  
      // 获取消息列表
      async fetchNotifications() {
        this.loading = true;
        try {
          // 实际项目中从API获取
          // const res = await this.$axios.get('/api/notifications', {
          //   params: {
          //     page: this.page,
          //     limit: this.pageSize,
          //     type: this.activeTab !== 'all' ? this.activeTab : undefined
          //   }
          // });
          // this.notifications = res.data.data.list;
          // this.hasMore = res.data.data.hasMore;
          
          // 模拟加载
          await new Promise(resolve => setTimeout(resolve, 500));
          this.loading = false;
        } catch (error) {
          console.error('获取消息失败:', error);
          this.$message.error('获取消息失败');
          this.loading = false;
        }
      },
  
      // 加载更多
      async loadMore() {
        this.loadingMore = true;
        this.page += 1;
        try {
          // 实际项目中从API获取更多
          await new Promise(resolve => setTimeout(resolve, 500));
          this.loadingMore = false;
        } catch (error) {
          console.error('加载更多失败:', error);
          this.loadingMore = false;
        }
      },
  
      // 处理消息点击
      handleNotificationClick(notification) {
        // 标记为已读
        this.markAsRead(notification.id);
        
        // 关闭抽屉
        this.drawerVisible = false;
        
        // 根据消息类型跳转
        if (notification.type === 'reply' || notification.type === 'like') {
          // 跳转到博客详情，并定位到评论
          this.$router.push({
            path: `/blog/${notification.targetId}`,
            query: {
              commentId: notification.commentId,
              highlight: true
            }
          });
        } else if (notification.type === 'system') {
          // 系统消息跳转到相关页面
          this.$router.push(`/blog/${notification.targetId}`);
        }
      },
  
      // 标记单条消息为已读
      markAsRead(id) {
        const notification = this.notifications.find(n => n.id === id);
        if (notification && !notification.isRead) {
          notification.isRead = true;
          // 实际项目中调用API
          // this.$axios.post(`/api/notifications/${id}/read`);
        }
      },
  
      // 全部标记为已读
      markAllAsRead() {
        this.notifications.forEach(n => n.isRead = true);
        this.$message.success('已全部标记为已读');
        // 实际项目中调用API
        // this.$axios.post('/api/notifications/read-all');
      },
  
      // 处理标签切换
      handleTabClick() {
        this.page = 1;
        this.fetchNotifications();
      },
  
      // 抽屉关闭前
      handleDrawerClose(done) {
        done();
      },
  
      // 获取消息类型文本
      getTypeText(type) {
        const typeMap = {
          reply: '回复',
          like: '点赞',
          system: '系统'
        };
        return typeMap[type] || type;
      },
  
      // 格式化时间
      formatTime(time) {
        if (!time) return '';
        const date = new Date(time);
        const now = new Date();
        const diff = Math.floor((now - date) / 1000);
        
        if (diff < 60) return '刚刚';
        if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
        if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
        if (diff < 2592000) return Math.floor(diff / 86400) + '天前';
        
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
      },
    },
  };
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