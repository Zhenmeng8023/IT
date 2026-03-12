<template>
  <div class="dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>博客仪表盘</h1>
      <p>实时监控博客数据，了解系统运行状况</p>
    </div>

    <!-- 数据概览卡片 -->
    <div class="overview-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-item">
              <div class="stat-icon" style="background: #409EFF;">
                <i class="el-icon-document"></i>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ overviewData.totalBlogs }}</div>
                <div class="stat-label">总博客数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-item">
              <div class="stat-icon" style="background: #67C23A;">
                <i class="el-icon-user"></i>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ overviewData.totalUsers }}</div>
                <div class="stat-label">总用户数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-item">
              <div class="stat-icon" style="background: #E6A23C;">
                <i class="el-icon-view"></i>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ overviewData.todayViews }}</div>
                <div class="stat-label">今日浏览量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-item">
              <div class="stat-icon" style="background: #F56C6C;">
                <i class="el-icon-chat-dot-round"></i>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ overviewData.pendingAudits }}</div>
                <div class="stat-label">待审核博客</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="chart-section">
      <el-row :gutter="20">
        <!-- 博客发布趋势 -->
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <span>博客发布趋势</span>
                <el-select v-model="trendPeriod" size="mini" style="width: 100px" @change="updateTrendData">
                  <el-option label="近7天" value="7"></el-option>
                  <el-option label="近30天" value="30"></el-option>
                  <el-option label="近90天" value="90"></el-option>
                </el-select>
              </div>
            </template>
            <div class="trend-chart">
              <div class="chart-labels">
                <div v-for="date in trendDates" :key="date" class="chart-label">{{ date }}</div>
              </div>
              <div class="chart-bars">
                <div v-for="(value, index) in blogTrendData" :key="index" class="bar-container">
                  <div class="bar-group">
                    <div 
                      class="bar blog-bar" 
                      :style="{ height: (value / 30 * 100) + '%' }"
                      :title="'发布数量: ' + value">
                    </div>
                    <div 
                      class="bar view-bar" 
                      :style="{ height: (viewTrendData[index] / 3000 * 100) + '%' }"
                      :title="'浏览量: ' + viewTrendData[index]">
                    </div>
                  </div>
                </div>
              </div>
              <div class="chart-legend">
                <div class="legend-item">
                  <span class="legend-color blog-color"></span>
                  <span>发布数量</span>
                </div>
                <div class="legend-item">
                  <span class="legend-color view-color"></span>
                  <span>浏览量</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 博客分类分布 -->
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <span>博客分类分布</span>
              </div>
            </template>
            <div class="category-chart">
              <div class="pie-chart">
                <div class="pie-slice" 
                     v-for="(item, index) in categoryData" 
                     :key="item.name"
                     :style="getPieSliceStyle(item, index)">
                </div>
              </div>
              <div class="category-legend">
                <div v-for="item in categoryData" :key="item.name" class="legend-item">
                  <span class="legend-color" :style="{backgroundColor: item.color}"></span>
                  <span class="legend-label">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }} ({{ getPercentage(item) }}%)</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 实时数据 -->
    <div class="realtime-section">
      <el-row :gutter="20">
        <!-- 热门博客 -->
        <el-col :span="12">
          <el-card class="realtime-card" shadow="never">
            <template #header>
              <div class="realtime-header">
                <span>热门博客排行</span>
                <el-button type="text" icon="el-icon-refresh" @click="refreshHotBlogs">刷新</el-button>
              </div>
            </template>
            <div class="hot-blog-list">
              <div v-for="(blog, index) in hotBlogs" :key="blog.id" class="hot-blog-item">
                <div class="blog-rank">
                  <span class="rank-number" :class="{'top3': index < 3}">{{ index + 1 }}</span>
                </div>
                <div class="blog-info">
                  <div class="blog-title">{{ blog.title }}</div>
                  <div class="blog-meta">
                    <span class="author">{{ blog.author }}</span>
                    <span class="views">{{ blog.viewCount }} 浏览</span>
                    <span class="time">{{ formatTime(blog.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 系统状态 -->
        <el-col :span="12">
          <el-card class="realtime-card" shadow="never">
            <template #header>
              <div class="realtime-header">
                <span>系统状态监控</span>
                <el-button type="text" icon="el-icon-refresh" @click="refreshSystemStatus">刷新</el-button>
              </div>
            </template>
            <div class="system-status">
              <div class="status-item">
                <div class="status-label">服务器状态</div>
                <div class="status-value">
                  <el-tag type="success" size="small">正常</el-tag>
                </div>
              </div>
              <div class="status-item">
                <div class="status-label">数据库连接</div>
                <div class="status-value">
                  <el-tag type="success" size="small">正常</el-tag>
                </div>
              </div>
              <div class="status-item">
                <div class="status-label">内存使用率</div>
                <div class="status-value">
                  <el-progress :percentage="systemStatus.memoryUsage" :stroke-width="8" style="width: 100px"></el-progress>
                </div>
              </div>
              <div class="status-item">
                <div class="status-label">CPU使用率</div>
                <div class="status-value">
                  <el-progress :percentage="systemStatus.cpuUsage" :stroke-width="8" style="width: 100px"></el-progress>
                </div>
              </div>
              <div class="status-item">
                <div class="status-label">在线用户</div>
                <div class="status-value">{{ systemStatus.onlineUsers }} 人</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 快速操作 -->
    <div class="quick-actions">
      <el-card class="actions-card" shadow="never">
        <template #header>
          <span>快速操作</span>
        </template>
        <div class="action-buttons">
          <el-button type="primary" icon="el-icon-s-check" @click="gotoAudit">审核博客</el-button>
          <el-button type="success" icon="el-icon-plus" @click="gotoCreate">创建博客</el-button>
          <el-button type="warning" icon="el-icon-user" @click="gotoUserManage">用户管理</el-button>
          <el-button type="info" icon="el-icon-setting" @click="gotoSettings">系统设置</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BlogDashboard',
  layout:"manage",
  data() {
    return {
      overviewData: {
        totalBlogs: 0,
        totalUsers: 0,
        todayViews: 0,
        pendingAudits: 0
      },
      
      trendPeriod: '7',
      blogTrendData: [12, 15, 8, 20, 18, 25, 22],
      viewTrendData: [1200, 1500, 800, 2000, 1800, 2500, 2200],
      trendDates: ['1月10日', '1月11日', '1月12日', '1月13日', '1月14日', '1月15日', '1月16日'],
      
      categoryData: [
        { name: '技术', value: 320, color: '#409EFF' },
        { name: '生活', value: 240, color: '#67C23A' },
        { name: '学习', value: 149, color: '#E6A23C' },
        { name: '其他', value: 100, color: '#909399' }
      ],
      
      hotBlogs: [],
      systemStatus: {
        memoryUsage: 0,
        cpuUsage: 0,
        onlineUsers: 0
      }
    }
  },
  
  mounted() {
    this.loadDashboardData()
  },
  
  methods: {
    // 加载仪表盘数据
    async loadDashboardData() {
      try {
        // 模拟数据加载
        this.overviewData = {
          totalBlogs: 1256,
          totalUsers: 342,
          todayViews: 2845,
          pendingAudits: 23
        }
        
        this.hotBlogs = [
          { id: 1, title: 'Vue3 组合式 API 最佳实践', author: '张三', viewCount: 1560, createTime: '2024-01-15' },
          { id: 2, title: 'Spring Boot 微服务架构设计', author: '李四', viewCount: 1289, createTime: '2024-01-14' },
          { id: 3, title: '机器学习入门指南', author: '王五', viewCount: 987, createTime: '2024-01-13' },
          { id: 4, title: 'React Hooks 深度解析', author: '赵六', viewCount: 856, createTime: '2024-01-12' },
          { id: 5, title: 'Docker 容器化部署', author: '钱七', viewCount: 743, createTime: '2024-01-11' }
        ]
        
        this.systemStatus = {
          memoryUsage: 65,
          cpuUsage: 42,
          onlineUsers: 28
        }
      } catch (error) {
        console.error('加载数据失败:', error)
        this.$message.error('加载数据失败')
      }
    },
    
    // 更新趋势数据
    updateTrendData() {
      // 根据选择的周期更新数据
      if (this.trendPeriod === '30') {
        this.blogTrendData = [10, 12, 8, 15, 20, 18, 22, 25, 20, 18, 15, 12, 10, 8, 12, 15, 18, 20, 22, 25, 28, 30, 25, 22, 20, 18, 15, 12, 10, 8]
        this.viewTrendData = [1000, 1200, 800, 1500, 2000, 1800, 2200, 2500, 2000, 1800, 1500, 1200, 1000, 800, 1200, 1500, 1800, 2000, 2200, 2500, 2800, 3000, 2500, 2200, 2000, 1800, 1500, 1200, 1000, 800]
        this.trendDates = Array.from({length: 30}, (_, i) => `1月${i+1}日`)
      } else if (this.trendPeriod === '90') {
        // 简化显示，只显示关键日期
        this.blogTrendData = [8, 12, 15, 18, 22, 25, 28, 30, 25, 22]
        this.viewTrendData = [800, 1200, 1500, 1800, 2200, 2500, 2800, 3000, 2500, 2200]
        this.trendDates = ['1月1日', '1月10日', '1月20日', '1月30日', '2月10日', '2月20日', '3月1日', '3月10日', '3月20日', '3月30日']
      } else {
        // 默认7天数据
        this.blogTrendData = [12, 15, 8, 20, 18, 25, 22]
        this.viewTrendData = [1200, 1500, 800, 2000, 1800, 2500, 2200]
        this.trendDates = ['1月10日', '1月11日', '1月12日', '1月13日', '1月14日', '1月15日', '1月16日']
      }
    },
    
    // 计算饼图切片样式
    getPieSliceStyle(item, index) {
      const total = this.categoryData.reduce((sum, d) => sum + d.value, 0)
      const percentage = (item.value / total) * 100
      const startAngle = this.categoryData.slice(0, index).reduce((sum, d) => sum + (d.value / total) * 360, 0)
      
      return {
        backgroundColor: item.color,
        transform: `rotate(${startAngle}deg)`,
        clipPath: `polygon(50% 50%, 50% 0%, ${50 + 50 * Math.cos((percentage * Math.PI) / 180)}% ${50 - 50 * Math.sin((percentage * Math.PI) / 180)}%)`
      }
    },
    
    // 计算百分比
    getPercentage(item) {
      const total = this.categoryData.reduce((sum, d) => sum + d.value, 0)
      return Math.round((item.value / total) * 100)
    },
    
    // 刷新热门博客
    refreshHotBlogs() {
      this.$message.success('热门博客数据已刷新')
      // 这里可以重新加载数据
    },
    
    // 刷新系统状态
    refreshSystemStatus() {
      this.$message.success('系统状态已刷新')
      // 这里可以重新加载数据
    },
    
    // 格式化时间
    formatTime(timeString) {
      if (!timeString) return ''
      return new Date(timeString).toLocaleDateString('zh-CN')
    },
    
    // 快速操作 - 跳转到审核页面
    gotoAudit() {
      this.$router.push('/audit')
    },
    
    // 快速操作 - 跳转到创建页面
    gotoCreate() {
      this.$message.info('跳转到创建博客页面')
    },
    
    // 快速操作 - 跳转到用户管理
    gotoUserManage() {
      this.$router.push('/usermanage')
    },
    
    // 快速操作 - 跳转到系统设置
    gotoSettings() {
      this.$message.info('跳转到系统设置页面')
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.overview-cards {
  margin: 20px 0;
}

.stat-card {
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon i {
  font-size: 24px;
  color: white;
}

.stat-content {
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

.chart-section {
  margin: 20px 0;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 趋势图样式 */
.trend-chart {
  height: 300px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.chart-labels {
  display: flex;
  justify-content: space-around;
  padding: 0 20px;
  margin-bottom: 10px;
}

.chart-label {
  font-size: 12px;
  color: #909399;
  text-align: center;
  flex: 1;
}

.chart-bars {
  flex: 1;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  padding: 0 20px;
  height: 200px;
}

.bar-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  height: 100%;
  position: relative;
}

.bar-group {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 100%;
}

.bar {
  width: 12px;
  border-radius: 3px 3px 0 0;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.bar:hover {
  opacity: 0.8;
  transform: scaleY(1.05);
}

.blog-bar {
  background: linear-gradient(to top, #409EFF, #79bbff);
}

.view-bar {
  background: linear-gradient(to top, #67C23A, #85ce61);
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #606266;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.blog-color {
  background: #409EFF;
}

.view-color {
  background: #67C23A;
}

/* 分类图样式 */
.category-chart {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pie-chart {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: #f5f7fa;
  position: relative;
  margin-right: 20px;
  overflow: hidden;
}

.pie-slice {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transform-origin: center;
}

.category-legend {
  flex: 1;
}

.category-legend .legend-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.category-legend .legend-item:hover {
  background-color: #f5f7fa;
}

.category-legend .legend-color {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  margin-right: 8px;
}

.category-legend .legend-label {
  flex: 1;
  font-size: 14px;
  color: #606266;
}

.category-legend .legend-value {
  font-size: 12px;
  color: #909399;
  font-weight: bold;
}

.realtime-section {
  margin: 20px 0;
}

.realtime-card {
  border-radius: 8px;
}

.realtime-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hot-blog-list {
  max-height: 300px;
  overflow-y: auto;
}

.hot-blog-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.hot-blog-item:last-child {
  border-bottom: none;
}

.blog-rank {
  width: 40px;
  text-align: center;
}

.rank-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  border-radius: 50%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  font-weight: bold;
}

.rank-number.top3 {
  background: #409EFF;
  color: white;
}

.blog-info {
  flex: 1;
}

.blog-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-meta {
  font-size: 12px;
  color: #909399;
}

.blog-meta span {
  margin-right: 10px;
}

.system-status {
  padding: 10px 0;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.status-item:last-child {
  border-bottom: none;
}

.status-label {
  color: #606266;
  font-size: 14px;
}

.status-value {
  font-size: 14px;
}

.quick-actions {
  margin: 20px 0;
}

.actions-card {
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .overview-cards .el-col {
    margin-bottom: 20px;
  }
  
  .chart-section .el-col,
  .realtime-section .el-col {
    margin-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .overview-cards .el-col {
    width: 100%;
  }
  
  .chart-section .el-col,
  .realtime-section .el-col {
    width: 100%;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
  
  .category-chart {
    flex-direction: column;
  }
  
  .pie-chart {
    margin-right: 0;
    margin-bottom: 20px;
  }
}
</style>