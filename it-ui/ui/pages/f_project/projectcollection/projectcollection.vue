<template>
  <div class="project-container">
    <!-- 页面头部 -->
    <div class="project-header">
      <h1 class="page-title">我的收藏</h1>
      <p class="page-subtitle">查看您收藏的精彩项目</p>
    </div>

    <!-- 加载状态 -->
    <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
      <!-- 收藏项目列表 - 卡片网格 -->
      <div v-if="!loading && collectedProjects.length > 0" class="project-grid">
        <el-card
          v-for="project in collectedProjects"
          :key="project.id"
          class="project-card"
          shadow="hover"
          @click.native="goToDetail(project.id)"
        >
          <div class="card-content">
            <!-- 项目标题 -->
            <h3 class="project-title">{{ project.title }}</h3>
            
            <!-- 项目类型和状态 -->
            <div class="project-meta">
              <el-tag 
                :type="getProjectTypeTag(project.type)" 
                size="small"
                class="type-tag"
              >
                {{ project.type }}
              </el-tag>
              <el-tag 
                :type="getStatusTag(project.status)" 
                size="small"
                class="status-tag"
              >
                {{ project.status }}
              </el-tag>
            </div>
            
            <!-- 作者信息 -->
            <div class="author-info">
              <el-avatar :size="24" :src="project.author?.avatar" class="author-avatar"></el-avatar>
              <span class="author-name">{{ project.author?.nickname || '未知作者' }}</span>
            </div>
            
            <!-- 技术栈标签 -->
            <div class="tech-stack" v-if="project.technologies && project.technologies.length > 0">
              <el-tag
                v-for="tech in project.technologies.slice(0, 3)"
                :key="tech"
                size="small"
                class="tech-tag"
                @click.stop="filterByTech(tech)"
              >
                {{ tech }}
              </el-tag>
              <span v-if="project.technologies.length > 3" class="more-tech">+{{ project.technologies.length - 3 }}更多</span>
            </div>
            
            <!-- 项目描述 -->
            <p class="project-description">{{ formatDescription(project.description) }}</p>
            
            <!-- 统计信息 -->
            <div class="project-stats">
              <span v-if="project.starCount !== undefined" class="stat-item">
                <i class="el-icon-star-off"></i>
                <span>{{ project.starCount }}</span>
              </span>
              <span v-if="project.forkCount !== undefined" class="stat-item">
                <i class="el-icon-share"></i>
                <span>{{ project.forkCount }}</span>
              </span>
              <span v-if="project.viewCount !== undefined" class="stat-item">
                <i class="el-icon-view"></i>
                <span>{{ project.viewCount }}</span>
              </span>
              <span v-if="project.updateTime" class="stat-item">
                <i class="el-icon-time"></i>
                <span>{{ formatTime(project.updateTime) }}</span>
              </span>
              <!-- 收藏时间 -->
              <span v-if="project.collectedTime" class="stat-item">
                <i class="el-icon-collection-tag"></i>
                <span>{{ formatTime(project.collectedTime) }}</span>
              </span>
            </div>
            
            <!-- 取消收藏按钮 -->
            <div class="collection-actions">
              <el-button 
                size="mini" 
                type="danger" 
                plain 
                @click.stop="removeFromCollection(project.id)"
                class="remove-btn"
              >
                <i class="el-icon-delete"></i>
                取消收藏
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && collectedProjects.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="el-icon-star-off"></i>
        </div>
        <h3 class="empty-title">暂无收藏项目</h3>
        <p class="empty-desc">您还没有收藏任何项目，快去发现精彩项目吧</p>
        <el-button type="primary" @click="goToProjectList" class="empty-action">
          浏览项目列表
        </el-button>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
// 导入项目相关的API接口
import { getParticipatedProjects } from '@/api/project'

export default {
  layout: 'project',
  data() {
    return {
      collectedProjects: [],
      total: 0,
      pageSize: 8,
      loading: false,
    };
  },
  computed: {
    currentPage: {
      get() {
        return parseInt(this.$route.query.page) || 1;
      },
      set(page) {
        const newQuery = { ...this.$route.query };
        if (page > 1) {
          newQuery.page = page;
        } else {
          delete newQuery.page;
        }
        this.$router.push({ query: newQuery });
      }
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.fetchCollections();
      },
      immediate: true
    }
  },
  methods: {
    // 获取收藏项目列表
    async fetchCollections() {
      this.loading = true;
      try {
        // 调用API获取参与的项目（作为收藏项目）
        const response = await getParticipatedProjects({
          page: this.currentPage,
          pageSize: this.pageSize
        });
        
        this.collectedProjects = response.data.items || [];
        this.total = response.data.total || 0;
      } catch (error) {
        console.error('获取收藏项目失败:', error);
        this.$message.error('获取收藏项目失败');
      } finally {
        this.loading = false;
      }
    },

    // 取消收藏
    async removeFromCollection(projectId) {
      try {
        // 这里可以调用退出项目的API
        // await quitProject(projectId);
        
        // 模拟取消收藏操作
        this.collectedProjects = this.collectedProjects.filter(p => p.id !== projectId);
        this.total = this.collectedProjects.length;
        
        this.$message.success('取消收藏成功');
      } catch (error) {
        console.error('取消收藏失败:', error);
        this.$message.error('取消收藏失败');
      }
    },

    // 跳转到项目详情
    goToDetail(projectId) {
      this.$router.push(`/f_project/projectdetail?projectId=${projectId}`);
    },

    // 跳转到项目列表
    goToProjectList() {
      this.$router.push('/f_project/list');
    },

    // 分页处理
    handlePageChange(page) {
      this.currentPage = page;
    },

    // 根据技术栈过滤
    filterByTech(tech) {
      // 跳转到项目列表并自动过滤该技术栈
      this.$router.push(`/f_project/list?tech=${encodeURIComponent(tech)}`);
    },

    // 格式化描述
    formatDescription(desc) {
      if (!desc) return '暂无描述';
      return desc.length > 100 ? desc.substring(0, 100) + '...' : desc;
    },

    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return date.toLocaleDateString('zh-CN');
    },

    // 获取项目类型标签样式
    getProjectTypeTag(type) {
      const typeMap = {
        'Web应用': 'primary',
        '移动应用': 'success',
        '桌面应用': 'warning',
        '工具库': 'info',
        '其他': 'danger'
      };
      return typeMap[type] || 'info';
    },

    // 获取状态标签样式
    getStatusTag(status) {
      const statusMap = {
        '已完成': 'success',
        '进行中': 'primary',
        '计划中': 'warning',
        '已暂停': 'info',
        '已废弃': 'danger'
      };
      return statusMap[status] || 'info';
    }
  }
};
</script>

<style scoped>
.project-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.project-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.loading-container {
  min-height: 400px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.project-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-content {
  padding: 16px;
}

.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.4;
}

.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  background-color: #409EFF;
}

.author-name {
  font-size: 14px;
  color: #606266;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.tech-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}

.tech-tag:hover {
  transform: scale(1.05);
}

.more-tech {
  font-size: 12px;
  color: #909399;
  align-self: center;
}

.project-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 16px;
  min-height: 42px;
}

.project-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.collection-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.remove-btn {
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  color: #DCDFE6;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  color: #606266;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.empty-action {
  margin-top: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .project-container {
    padding: 16px;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .page-title {
    font-size: 24px;
  }
}
</style>