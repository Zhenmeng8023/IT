<template>
  <div class="project-container">
    <!-- 页面头部 -->
    <div class="project-header">
      <h1 class="page-title">项目列表</h1>
      <p class="page-subtitle">探索精彩的技术项目，发现创新的解决方案</p>
    </div>

    <!-- 排序工具栏 - 美化版 -->
    <div class="sort-toolbar">
      <div class="sort-wrapper">
        <span class="sort-label">排序：</span>
        <el-radio-group v-model="sortType" size="small" @change="handleSortChange" class="sort-group">
          <el-radio-button label="hot">
            <i class="el-icon-fire"></i> 热门
          </el-radio-button>
          <el-radio-button label="time_desc">
            <i class="el-icon-download"></i> 最新
          </el-radio-button>
          <el-radio-button label="time_asc">
            <i class="el-icon-upload"></i> 最早
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
      <!-- 项目列表 - 卡片网格 -->
      <div v-if="!loading && projects.length > 0" class="project-grid">
        <el-card
          v-for="project in projects"
          :key="project.id"
          class="project-card"
          shadow="hover"
          @click.native="goToDetail(project.id)"
        >
          <div class="card-content">
            <!-- 项目标题 -->
            <h3 class="project-title">{{ project.name || project.title }}</h3>
            
            <!-- 项目类型和状态 -->
            <div class="project-meta">
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
              <el-avatar :size="24" :src="(project.author?.avatar || project.authorAvatar || authorMap[project.authorId || project.author_id]?.avatar)" class="author-avatar"></el-avatar>
              <span class="author-name">{{ (project.author?.nickname || project.author?.username || project.authorName || authorMap[project.authorId || project.author_id]?.nickname || authorMap[project.authorId || project.author_id]?.username || authorMap[project.authorId || project.author_id]?.name || '未知作者') }}</span>
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
              <span v-if="project.stars !== undefined || project.starCount !== undefined" class="stat-item">
                <i class="el-icon-star-off"></i>
                <span>{{ project.stars || project.starCount }}</span>
              </span>
              <span v-if="project.downloads !== undefined || project.forkCount !== undefined" class="stat-item">
                <i class="el-icon-share"></i>
                <span>{{ project.downloads || project.forkCount }}</span>
              </span>
              <span v-if="project.views !== undefined || project.viewCount !== undefined" class="stat-item">
                <i class="el-icon-view"></i>
                <span>{{ project.views || project.viewCount }}</span>
              </span>
              <span v-if="project.updatedAt || project.updateTime" class="stat-item">
                <i class="el-icon-time"></i>
                <span>{{ formatTime(project.updatedAt || project.updateTime) }}</span>
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && projects.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="el-icon-s-management"></i>
        </div>
        <h3 class="empty-title">暂无项目</h3>
        <p class="empty-desc">还没有任何项目，快去创建第一个项目吧</p>
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
import { pageProjects } from '@/api/project'

// 导入用户相关的API接口
import request from '@/utils/request'

// 获取用户信息
const getUserInfo = async (userId) => {
  try {
    console.log('获取用户信息:', userId)
    // 尝试不同的API路径
    let response
    try {
      // 尝试第一种路径（正确的路径）
      response = await request({
        url: `/users/${userId}`,
        method: 'get'
      })
    } catch (e) {
      console.log('第一种路径失败，尝试第二种路径')
      // 尝试第二种路径
      response = await request({
        url: `/api/users/${userId}`,
        method: 'get'
      })
    }
    console.log('用户信息响应:', response)
    return response.data
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return null
  }
}

export default {
  layout: 'project',
  data() {
    return {
      projects: [],
      total: 0,
      pageSize: 8,
      loading: false,
      sortType: 'time_desc',
      authorMap: {}, // 存储作者信息的映射
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
        this.fetchProjects();
      },
      immediate: true
    }
  },
  methods: {
    // 获取项目列表
    async fetchProjects() {
      this.loading = true;
      try {
        // 调用API获取项目列表
        const response = await pageProjects({
          page: this.currentPage,
          pageSize: this.pageSize,
          sortType: this.sortType,
          keyword: this.$route.query.tech || ''
        });
        
        this.projects = response.data.list || response.data.items || [];
        this.total = response.data.total || 0;
        
        // 如果没有数据，添加模拟数据
        if (this.projects.length === 0) {
          this.projects = [
            {
            id: 1,
            name: '博客管理系统',
            type: 'Web应用',
            status: '已完成',
            description: '一个基于Vue.js和Node.js的现代化博客管理系统，支持多用户、标签管理、评论系统等功能。',
            technologies: ['Vue.js', 'Node.js', 'MongoDB', 'Express', 'Element UI'],
            authorId: 1,
            authorName: '开发者A',
            authorAvatar: '',
            starCount: 45,
            forkCount: 12,
            viewCount: 123,
            updateTime: '2024-03-20T14:30:00Z'
          },
          {
            id: 2,
            name: '在线商城系统',
            type: 'Web应用',
            status: '开发中',
            description: '一个基于React和Spring Boot的在线商城系统，支持商品管理、订单处理、支付功能等。',
            technologies: ['React', 'Spring Boot', 'MySQL', 'Redis', 'Ant Design'],
            authorId: 2,
            authorName: '开发者B',
            authorAvatar: '',
            starCount: 23,
            forkCount: 8,
            viewCount: 89,
            updateTime: '2024-03-18T10:20:00Z'
          },
          {
            id: 3,
            name: '移动应用框架',
            type: '移动应用',
            status: '已完成',
            description: '一个基于Flutter的跨平台移动应用开发框架，支持iOS和Android平台。',
            technologies: ['Flutter', 'Dart', 'Firebase', 'REST API'],
            authorId: 3,
            authorName: '开发者C',
            authorAvatar: '',
            starCount: 67,
            forkCount: 15,
            viewCount: 201,
            updateTime: '2024-03-15T09:15:00Z'
          }
          ];
          this.total = 3;
        }
        
        // 对项目进行排序
        this.sortProjects();
        
        // 获取作者信息
        await this.fetchAuthorInfo();
        
      } catch (error) {
        console.error('获取项目列表失败:', error);
        this.$message.error('获取项目列表失败');
        
        // 错误时添加模拟数据
        this.projects = [
          {
            id: 1,
            name: '博客管理系统',
            type: 'Web应用',
            status: '已完成',
            description: '一个基于Vue.js和Node.js的现代化博客管理系统，支持多用户、标签管理、评论系统等功能。',
            technologies: ['Vue.js', 'Node.js', 'MongoDB', 'Express', 'Element UI'],
            authorId: 1,
            authorName: '开发者A',
            authorAvatar: '',
            starCount: 45,
            forkCount: 12,
            viewCount: 123,
            updateTime: '2024-03-20T14:30:00Z'
          },
          {
            id: 2,
            name: '在线商城系统',
            type: 'Web应用',
            status: '开发中',
            description: '一个基于React和Spring Boot的在线商城系统，支持商品管理、订单处理、支付功能等。',
            technologies: ['React', 'Spring Boot', 'MySQL', 'Redis', 'Ant Design'],
            authorId: 2,
            authorName: '开发者B',
            authorAvatar: '',
            starCount: 23,
            forkCount: 8,
            viewCount: 89,
            updateTime: '2024-03-18T10:20:00Z'
          }
        ];
        this.total = 2;
        
        // 对项目进行排序
        this.sortProjects();
        
        // 获取作者信息
        await this.fetchAuthorInfo();
      } finally {
        this.loading = false;
      }
    },
    
    // 对项目进行排序
    sortProjects() {
      console.log('排序类型:', this.sortType);
      console.log('排序前项目顺序:', this.projects.map(p => ({id: p.id, name: p.name, updateTime: p.updateTime})));
      
      switch (this.sortType) {
        case 'hot':
          // 按热度排序（综合考虑收藏数、复刻数和浏览数）
          this.projects.sort((a, b) => {
            const hotnessA = (a.starCount || 0) * 3 + (a.forkCount || 0) * 2 + (a.viewCount || 0);
            const hotnessB = (b.starCount || 0) * 3 + (b.forkCount || 0) * 2 + (b.viewCount || 0);
            return hotnessB - hotnessA;
          });
          break;
        case 'time_desc':
          // 按时间降序排序（最新）
          this.projects.sort((a, b) => {
            const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
            const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
            return timeB - timeA;
          });
          break;
        case 'time_asc':
          // 按时间升序排序（最早）
          this.projects.sort((a, b) => {
            const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
            const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
            return timeA - timeB;
          });
          break;
        default:
          break;
      }
      
      console.log('排序后项目顺序:', this.projects.map(p => ({id: p.id, name: p.name, updateTime: p.updateTime})));
    },
    
    // 获取作者信息
    async fetchAuthorInfo() {
      console.log('开始获取作者信息')
      console.log('项目列表:', this.projects)
      
      // 同时支持authorId和author_id字段
      const authorIds = this.projects
        .map(project => project.authorId || project.author_id)
        .filter(id => id && !this.authorMap[id]);
      
      console.log('需要获取的作者ID:', authorIds)
      
      if (authorIds.length > 0) {
        // 并行获取作者信息
        const authorPromises = authorIds.map(id => getUserInfo(id));
        const authorResults = await Promise.all(authorPromises);
        
        console.log('作者信息结果:', authorResults)
        
        // 存储作者信息
        authorResults.forEach((author, index) => {
          if (author) {
            console.log('存储作者信息:', authorIds[index], author)
            this.authorMap[authorIds[index]] = author;
          }
        });
      }
      
      console.log('最终作者信息映射:', this.authorMap)
    },

    // 排序变化处理
    handleSortChange() {
      this.currentPage = 1;
      this.fetchProjects();
    },

    // 分页变化处理
    handlePageChange(page) {
      this.currentPage = page;
    },

    // 跳转到项目详情
    goToDetail(projectId) {
      this.$router.push(`/projectdetail?projectId=${projectId}`);
    },

    // 根据技术栈筛选
    filterByTech(tech) {
      this.$router.push({
        path: '/f_project/list',
        query: { tech }
      });
    },

    // 格式化项目描述
    formatDescription(description) {
      if (!description) return '';
      return description.length > 100 ? description.substring(0, 100) + '...' : description;
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return `${date.getMonth() + 1}月${date.getDate()}日`;
    },

    // 获取项目类型标签样式
    getProjectTypeTag(type) {
      const typeMap = {
        'Web应用': 'primary',
        '移动应用': 'success',
        'AI应用': 'warning',
        '工具库': 'info',
        '其他': 'default'
      };
      return typeMap[type] || 'default';
    },

    // 获取状态标签样式
    getStatusTag(status) {
      const statusMap = {
        '已完成': 'success',
        '开发中': 'warning',
        '维护中': 'info',
        '已归档': 'default',
        '暂停': 'danger'
      };
      return statusMap[status] || 'default';
    }
  }
};
</script>

<style scoped>
/* ========== 容器样式 ========== */
.project-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
}

/* ========== 页面头部 ========== */
.project-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 10px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

/* ========== 排序工具栏 ========== */
.sort-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
  background: white;
  padding: 12px 20px;
  border-radius: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.sort-label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.sort-group :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  transition: all 0.3s ease;
}

.sort-group :deep(.el-radio-button__inner i) {
  margin-right: 4px;
  font-size: 12px;
}

.sort-group :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.sort-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 20px 0 0 20px;
}

.sort-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 20px 20px 0;
}

/* ========== 项目网格 ========== */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.project-card {
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  background: white;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05);
  border-color: rgba(59, 130, 246, 0.2);
}

.card-content {
  padding: 24px;
}

/* 项目标题 */
.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px;
  line-height: 1.4;
  transition: color 0.2s ease;
}

.project-card:hover .project-title {
  color: #3b82f6;
}

/* 项目元信息 */
.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.type-tag, .status-tag {
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

/* 作者信息 */
.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  transition: border-color 0.3s ease;
}

.project-card:hover .author-avatar {
  border-color: #3b82f6;
}

.author-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

/* 技术栈 */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  align-items: center;
}

.tech-tag {
  background: #f1f5f9;
  border: none;
  color: #475569;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tech-tag:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
}

.more-tech {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 4px;
}

/* 项目描述 */
.project-description {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 统计信息 */
.project-stats {
  display: flex;
  gap: 15px;
  padding-top: 15px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-item i {
  font-size: 14px;
  color: #94a3b8;
  transition: color 0.2s ease;
}

.stat-item:hover i {
  color: #3b82f6;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 24px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  max-width: 400px;
  margin: 50px auto;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

.pagination-wrapper :deep(.el-pagination) {
  font-weight: 400;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #3b82f6;
  background: #f1f5f9;
}

/* ========== 加载状态 ========== */
.loading-container {
  min-height: 400px;
}

/* ========== 响应式 ========== */
@media screen and (max-width: 768px) {
  .project-container {
    padding: 20px 15px;
  }

  .page-title {
    font-size: 28px;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .sort-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .sort-group {
    width: 100%;
  }

  .sort-group :deep(.el-radio-group) {
    display: flex;
    width: 100%;
  }

  .sort-group :deep(.el-radio-button) {
    flex: 1;
  }

  .sort-group :deep(.el-radio-button__inner) {
    width: 100%;
    text-align: center;
  }
}

@media screen and (max-width: 480px) {
  .project-stats {
    flex-wrap: wrap;
    gap: 10px;
  }

  .project-title {
    font-size: 16px;
  }

  .card-content {
    padding: 16px;
  }
}
</style>