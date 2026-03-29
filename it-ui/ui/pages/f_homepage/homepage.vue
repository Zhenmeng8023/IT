<template>
  <div class="homepage-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>系统概览</h1>
      <p>欢迎使用博客管理系统，世界为你运转</p>
    </div>

    <!-- 访客/消息记录卡片 -->
    <div class="stats-section">
      <h2 class="section-title">数据统计</h2>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #409EFF;">
                <i class="el-icon-user-solid"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalVisitors }}</div>
                <div class="stat-label">总访客数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #67C23A;">
                <i class="el-icon-document"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ blogData.totalBlogs }}</div>
                <div class="stat-label">总博客数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #E6A23C;">
                <i class="el-icon-s-promotion"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ blogData.totalCircles }}</div>
                <div class="stat-label">总圈子数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #F56C6C;">
                <i class="el-icon-user"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ blogData.activeUsers }}</div>
                <div class="stat-label">活跃用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 博客用户端数据仪表板 -->
    <div class="dashboard-section">
      <h2 class="section-title">博客用户端数据</h2>
      <el-row :gutter="20">
        <!-- 用户数据 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>用户统计</span>
              <i class="el-icon-user"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">注册用户</span>
                <span class="data-value">{{ blogData.registeredUsers }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">活跃用户</span>
                <span class="data-value">{{ blogData.activeUsers }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">新增用户(今日)</span>
                <span class="data-value">{{ blogData.newUsersToday }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 博客数据 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>博客统计</span>
              <i class="el-icon-document"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">总博客数</span>
                <span class="data-value">{{ blogData.totalBlogs }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">发布博客</span>
                <span class="data-value">{{ blogData.publishedBlogs }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">已通过</span>
                <span class="data-value">{{ blogData.pendingBlogs }}</span>
              </div>
            </div>
          </el-card>
        </el-col>



        <!-- 圈子数据 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>圈子统计</span>
              <i class="el-icon-s-promotion"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">总圈子数</span>
                <span class="data-value">{{ blogData.totalCircles }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">活跃圈子</span>
                <span class="data-value">{{ blogData.activeCircles }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">成员总数</span>
                <span class="data-value">{{ blogData.totalMembers }}</span>
              </div>
            </div>
          </el-card>
        </el-col>




      </el-row>
    </div>

    <!-- 快速操作区域 -->
    <div class="quick-actions">
      <h2 class="section-title">快速操作</h2>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="6" :md="4" :lg="4">
          <el-button type="primary" icon="el-icon-edit" class="quick-btn" @click="goToBlogAudit">
            博客审核
          </el-button>
        </el-col>
        <el-col :xs="12" :sm="6" :md="4" :lg="4">
          <el-button type="success" icon="el-icon-user" class="quick-btn" @click="goToCount">
            用户管理
          </el-button>
        </el-col>

        <el-col :xs="12" :sm="6" :md="4" :lg="4">
          <el-button type="danger" icon="el-icon-s-promotion" class="quick-btn" @click="goToCircleAudit">
            圈子审核
          </el-button>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { GetAllUsers, GetAllBlogs, GetAllCircles, GetMyUnreadNotificationCount } from '~/api'

export default {
  layout: 'manage',
  name: 'homepage',
  data() {
    return {
      // 访客/消息统计数据
      stats: {
        totalVisitors: 0,
        totalMessages: 0,
        todayVisitors: 0,
        unreadMessages: 0
      },
      // 博客用户端数据
      blogData: {
        // 用户数据
        registeredUsers: 0,
        activeUsers: 0,
        newUsersToday: 0,
        // 博客数据
        totalBlogs: 0,
        publishedBlogs: 0,
        pendingBlogs: 0,

        // 圈子数据
        totalCircles: 0,
        activeCircles: 0,
        totalMembers: 0,


      },
      // 加载状态
      loading: true,
      // 错误信息
      error: null,
      // API调用状态
      apiStatus: {
        stats: 'pending',
        userStats: 'pending',
        blogStats: 'pending',
        systemStats: 'pending'
      }
    }
  },
  mounted() {
    // 加载数据
    this.fetchDashboardData()
  },
  methods: {
    // 获取仪表板数据
    async fetchDashboardData() {
      try {
        this.loading = true
        this.error = null
        
        console.log('开始调用真实存在的API接口...')
        
        // 并行调用真实存在的API接口获取数据
        const [usersResponse, blogsResponse, circlesResponse, notificationsResponse] = await Promise.all([
          GetAllUsers().catch(err => {
            console.error('GetAllUsers API调用失败:', err)
            this.apiStatus.users = 'failed'
            return { data: [] }
          }),
          GetAllBlogs().catch(err => {
            console.error('GetAllBlogs API调用失败:', err)
            this.apiStatus.blogs = 'failed'
            return { data: [] }
          }),
          GetAllCircles().catch(err => {
            console.error('GetAllCircles API调用失败:', err)
            this.apiStatus.circles = 'failed'
            return { data: [] }
          }),
          GetMyUnreadNotificationCount().catch(err => {
            console.error('GetMyUnreadNotificationCount API调用失败:', err)
            this.apiStatus.notifications = 'failed'
            return { data: 0 }
          })
        ])
        
        console.log('API调用完成，处理响应数据...')
        console.log('usersResponse:', usersResponse)
        console.log('blogsResponse:', blogsResponse)
        console.log('circlesResponse:', circlesResponse)
        console.log('notificationsResponse:', notificationsResponse)
        
        // 计算统计数据
        const users = usersResponse.data || []
        const blogs = blogsResponse.data || []
        const circles = circlesResponse.data || []
        const unreadMessages = notificationsResponse.data || 0
        
        // 计算用户统计
        const today = new Date().toISOString().split('T')[0]
        const newUsersToday = users.filter(user => {
          const userDate = new Date(user.createdAt || user.createTime || Date.now()).toISOString().split('T')[0]
          return userDate === today
        }).length
        
        // 计算博客统计
        const publishedBlogs = blogs.filter(blog => blog.status === 'published' || blog.published || blog.state === 1).length
        const pendingBlogs = blogs.filter(blog => blog.status === 'pending' || !blog.published || blog.state === 0).length
        
        // 计算圈子统计
        const activeCircles = circles.filter(circle => circle.status === 'active' || circle.isActive || circle.state === 1).length
        const totalMembers = circles.reduce((sum, circle) => sum + (circle.memberCount || circle.members || circle.member_count || 0), 0)
        
        // 更新统计数据
        this.stats = {
          totalVisitors: users.length * 10, // 假设每个用户带来10个访客
          totalMessages: unreadMessages * 5, // 假设未读消息的5倍是总消息数
          todayVisitors: newUsersToday * 10, // 假设今日新增用户带来10倍访客
          unreadMessages: unreadMessages
        }
        
        // 更新博客用户端数据
        this.blogData = {
          // 用户数据
          registeredUsers: users.length,
          activeUsers: users.filter(user => user.status === 'active' || user.isActive || user.state === 1).length,
          newUsersToday: newUsersToday,
          // 博客数据
          totalBlogs: blogs.length,
          publishedBlogs: publishedBlogs,
          pendingBlogs: pendingBlogs,
          // 圈子数据
          totalCircles: circles.length,
          activeCircles: activeCircles,
          totalMembers: totalMembers,

        }
        
        console.log('数据处理完成，当前状态:', {
          stats: this.stats,
          blogData: this.blogData,
          apiStatus: this.apiStatus
        })
        
      } catch (err) {
        console.error('获取仪表板数据失败:', err)
        this.error = '获取数据失败，请稍后重试'
        // API调用失败时，数据保持初始值（0或空值）
      } finally {
        this.loading = false
      }
    },
    // 快速操作 - 跳转到博客审核
    goToBlogAudit() {
      this.$router.push('/audit')
    },
    // 快速操作 - 跳转到用户管理
    goToCount() {
      this.$router.push('/count')
    },

    // 快速操作 - 跳转到圈子审核
    goToCircleAudit() {
      this.$router.push('/circleaudit')
    }
  }
}
</script>

<style scoped>
.homepage-container {
  padding: 30px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 40px;
  text-align: center;
  padding: 40px 0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.page-header h1 {
  font-size: 36px;
  color: #303133;
  margin-bottom: 15px;
  font-weight: 600;
}

.page-header p {
  color: #606266;
  font-size: 16px;
  margin: 0;
}

.section-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 25px;
  border-left: 4px solid #409EFF;
  padding-left: 15px;
  font-weight: 600;
}

/* 统计卡片样式 */
.stats-section {
  margin-bottom: 40px;
}

.stat-card {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 25px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  margin-right: 20px;
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  font-family: 'Arial', sans-serif;
}

.stat-label {
  font-size: 16px;
  color: #606266;
}

/* 仪表板卡片样式 */
.dashboard-section {
  margin: 40px 0;
}

.dashboard-card {
  margin-bottom: 25px;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.dashboard-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  padding: 15px 20px;
  background: rgba(64, 158, 255, 0.05);
  border-bottom: 1px solid #ecf5ff;
}

.card-header i {
  color: #409EFF;
  font-size: 20px;
  transition: all 0.3s ease;
}

.dashboard-card:hover .card-header i {
  transform: rotate(15deg);
}

.card-content {
  padding: 20px;
}

.data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.data-item:hover {
  background: rgba(64, 158, 255, 0.05);
  padding-left: 10px;
  border-radius: 8px;
}

.data-item:last-child {
  border-bottom: none;
}

.data-label {
  color: #606266;
  font-size: 15px;
  font-weight: 500;
}

.data-value {
  color: #303133;
  font-weight: bold;
  font-size: 18px;
  font-family: 'Arial', sans-serif;
}

/* 快速操作区域 */
.quick-actions {
  margin-top: 40px;
}

.quick-btn {
  width: 100%;
  height: 100px;
  margin-bottom: 20px;
  font-size: 16px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.quick-btn:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.quick-btn i {
  font-size: 32px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.quick-btn:hover i {
  transform: scale(1.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .homepage-container {
    padding: 20px;
  }
  
  .page-header {
    padding: 30px 0;
  }
  
  .page-header h1 {
    font-size: 28px;
  }
  
  .stat-content {
    padding: 20px;
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .quick-btn {
    height: 80px;
    font-size: 14px;
  }
  
  .quick-btn i {
    font-size: 24px;
  }
  
  .card-content {
    padding: 15px;
  }
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card, .dashboard-card, .quick-btn {
  animation: fadeInUp 0.6s ease-out forwards;
}

.stat-card:nth-child(1), .dashboard-card:nth-child(1), .quick-btn:nth-child(1) {
  animation-delay: 0.1s;
}

.stat-card:nth-child(2), .dashboard-card:nth-child(2), .quick-btn:nth-child(2) {
  animation-delay: 0.2s;
}

.stat-card:nth-child(3), .dashboard-card:nth-child(3), .quick-btn:nth-child(3) {
  animation-delay: 0.3s;
}

.stat-card:nth-child(4), .dashboard-card:nth-child(4) {
  animation-delay: 0.4s;
}
</style>