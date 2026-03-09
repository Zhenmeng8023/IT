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
                <i class="el-icon-chat-dot-round"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalMessages }}</div>
                <div class="stat-label">总消息数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #E6A23C;">
                <i class="el-icon-notebook-2"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.todayVisitors }}</div>
                <div class="stat-label">今日访客</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" style="background-color: #F56C6C;">
                <i class="el-icon-bell"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.unreadMessages }}</div>
                <div class="stat-label">未读消息</div>
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
                <span class="data-label">待审核</span>
                <span class="data-value">{{ blogData.pendingBlogs }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 项目数据 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>项目统计</span>
              <i class="el-icon-s-management"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">总项目数</span>
                <span class="data-value">{{ blogData.totalProjects }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">进行中</span>
                <span class="data-value">{{ blogData.ongoingProjects }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">已完成</span>
                <span class="data-value">{{ blogData.completedProjects }}</span>
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

        <!-- 访问数据 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>访问统计</span>
              <i class="el-icon-data-analysis"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">PV(今日)</span>
                <span class="data-value">{{ blogData.todayPV }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">UV(今日)</span>
                <span class="data-value">{{ blogData.todayUV }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">平均停留</span>
                <span class="data-value">{{ blogData.avgStayTime }}s</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 系统状态 -->
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-card class="dashboard-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>系统状态</span>
              <i class="el-icon-s-platform"></i>
            </div>
            <div class="card-content">
              <div class="data-item">
                <span class="data-label">运行时间</span>
                <span class="data-value">{{ blogData.uptime }}</span>
              </div>
              <div class="data-item">
                <span class="data-label">CPU使用率</span>
                <span class="data-value">{{ blogData.cpuUsage }}%</span>
              </div>
              <div class="data-item">
                <span class="data-label">内存使用率</span>
                <span class="data-value">{{ blogData.memoryUsage }}%</span>
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
          <el-button type="success" icon="el-icon-user" class="quick-btn" @click="goToUserManage">
            用户管理
          </el-button>
        </el-col>
        <el-col :xs="12" :sm="6" :md="4" :lg="4">
          <el-button type="warning" icon="el-icon-s-management" class="quick-btn" @click="goToProjectAudit">
            项目审核
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
export default {
  layout: 'manage',
  name: 'homepage',
  data() {
    return {
      // 访客/消息统计数据
      stats: {
        totalVisitors: 12560,
        totalMessages: 3421,
        todayVisitors: 256,
        unreadMessages: 23
      },
      // 博客用户端数据
      blogData: {
        // 用户数据
        registeredUsers: 8560,
        activeUsers: 2345,
        newUsersToday: 128,
        // 博客数据
        totalBlogs: 12560,
        publishedBlogs: 11234,
        pendingBlogs: 326,
        // 项目数据
        totalProjects: 456,
        ongoingProjects: 234,
        completedProjects: 222,
        // 圈子数据
        totalCircles: 123,
        activeCircles: 89,
        totalMembers: 5678,
        // 访问数据
        todayPV: 12560,
        todayUV: 2345,
        avgStayTime: 156,
        // 系统状态
        uptime: '15天8小时',
        cpuUsage: 45,
        memoryUsage: 68
      }
    }
  },
  mounted() {
    // 可以在这里添加数据获取逻辑
    this.fetchDashboardData()
  },
  methods: {
    // 获取仪表板数据
    fetchDashboardData() {
      // 这里可以调用API获取实时数据
      // 暂时使用模拟数据
      console.log('获取仪表板数据...')
    },
    // 快速操作 - 跳转到博客审核
    goToBlogAudit() {
      this.$router.push('/audit')
    },
    // 快速操作 - 跳转到用户管理
    goToUserManage() {
      this.$router.push('/usermanage')
    },
    // 快速操作 - 跳转到项目审核
    goToProjectAudit() {
      this.$router.push('/projectaudit')
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
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100%;
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.page-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
}

.page-header p {
  color: #909399;
  font-size: 14px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 20px;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

/* 统计卡片样式 */
.stat-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 仪表板卡片样式 */
.dashboard-section {
  margin: 30px 0;
}

.dashboard-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.card-header i {
  color: #409EFF;
  font-size: 18px;
}

.card-content {
  padding: 10px 0;
}

.data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.data-item:last-child {
  border-bottom: none;
}

.data-label {
  color: #606266;
  font-size: 14px;
}

.data-value {
  color: #303133;
  font-weight: bold;
  font-size: 16px;
}

/* 快速操作区域 */
.quick-actions {
  margin-top: 30px;
}

.quick-btn {
  width: 100%;
  height: 80px;
  margin-bottom: 20px;
  font-size: 14px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.quick-btn i {
  font-size: 24px;
  margin-bottom: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .homepage-container {
    padding: 10px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .quick-btn {
    height: 60px;
    font-size: 12px;
  }
  
  .quick-btn i {
    font-size: 20px;
  }
}
</style>