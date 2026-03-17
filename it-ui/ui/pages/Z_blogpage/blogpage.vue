<template>
  <div class="blog-container">
    <!-- 页面头部 -->
    <div class="blog-header">
      <h1 class="page-title">技术博客</h1>
      <p class="page-subtitle">发现最新的技术文章，与开发者一起成长</p>
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
      <!-- 博客列表 - 卡片网格 -->
      <div v-if="!loading && posts.length > 0" class="blog-grid">
        <el-card
          v-for="post in posts"
          :key="post.id"
          class="blog-card"
          shadow="hover"
          @click.native="goToDetail(post.id)"
        >
          <div class="card-content">
            <!-- 标题 -->
            <h3 class="blog-title">{{ post.title || '无标题' }}</h3>
            
            <!-- 作者信息 -->
            <div class="author-info">
              <el-avatar :size="24" :src="post.author?.avatar" class="author-avatar"></el-avatar>
              <span class="author-name">{{ post.author?.nickname || '未知作者' }}</span>
            </div>
            
            <!-- 标签区域 -->
            <div class="tags-container" v-if="post.tags && post.tags.length > 0">
              <el-tag
                v-for="tag in post.tags"
                :key="tag"
                size="small"
                class="tag-item"
                @click.stop="filterByTag(tag)"
              >
                #{{ tag }}
              </el-tag>
            </div>
            
            <!-- 内容预览 -->
            <p class="blog-excerpt">{{ post.content ? (post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content) : '' }}</p>
            
            <!-- 统计信息 -->
            <div class="post-stats">
              <span v-if="post.viewCount !== undefined" class="stat-item">
                <i class="el-icon-view"></i>
                <span>{{ post.viewCount }}</span>
              </span>
              <span v-if="post.likeCount !== undefined" class="stat-item">
                <i class="el-icon-star-off"></i>
                <span>{{ post.likeCount }}</span>
              </span>
              <span v-if="post.commentCount !== undefined" class="stat-item">
                <i class="el-icon-chat-line-round"></i>
                <span>{{ post.commentCount }}</span>
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && posts.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="el-icon-document"></i>
        </div>
        <h3 class="empty-title">暂无博客</h3>
        <p class="empty-desc">还没有任何博客文章，去看看其他内容吧</p>
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
import { GetAllBlogs, SearchBlogs, SearchBlogsByTag, SearchBlogsByAuthor, SortBlogs } from '@/api/index'
export default {
  layout:'blog',
  data() {
    return {
      posts: [],
      total: 0,
      pageSize: 5,
      loading: false,
      sortType: 'time_desc',
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
        
        if (Object.keys(newQuery).length === 0) {
          this.$router.push({ path: this.$route.path });
        } else {
          this.$router.push({ query: newQuery });
        }
      },
    },
    author() {
      return this.$route.query.author || '';
    },
    keyword() {
      return this.$route.query.keyword || '';
    },
    tag() {
      return this.$route.query.tag || '';
    },
    sortFromRoute() {
      return this.$route.query.sort || 'time_desc';
    },
  },
  watch: {
    '$route.query': {
      handler() {
        this.sortType = this.sortFromRoute;
        this.fetchPosts();
      },
      deep: true,
      immediate: true,
    },
  },
  methods: {
    goToDetail(id) {
      this.$router.push(`/blog/${id}`);
    },
    
    filterByTag(tag) {
      this.$router.push({
        query: {
          ...this.$route.query,
          tag: tag,
          page: 1,
        }
      });
    },
    
    handleSortChange() {
      this.$router.push({
        query: {
          ...this.$route.query,
          sort: this.sortType,
          page: 1,
        }
      });
    },
    
    async getSortedBlogs() {
      try {
        console.log(`获取排序后的博客列表，排序方式: ${this.sortType}`);
        
        let sortTypeMap = {
          'hot': 'hot',
          'time_asc': 'time/oldest',
          'time_desc': 'time/newest'
        };
        
        const sortType = sortTypeMap[this.sortType] || 'time/newest';
        const params = {
          page: this.currentPage,
          limit: this.pageSize
        };
        
        const apiResponse = await SortBlogs(sortType, params);
        
        console.log('排序博客API响应:', apiResponse);
        
        if (apiResponse && typeof apiResponse === 'object') {
          if (Array.isArray(apiResponse)) {
            this.posts = apiResponse;
            this.total = apiResponse.length;
          } else if (apiResponse.data && Array.isArray(apiResponse.data.list)) {
            this.posts = apiResponse.data.list || [];
            this.total = apiResponse.data.total || 0;
          } else if (Array.isArray(apiResponse.data)) {
            this.posts = apiResponse.data || [];
            this.total = apiResponse.data.length || 0;
          } else {
            this.posts = apiResponse.data || [];
            this.total = apiResponse.data?.length || 0;
          }
          return;
        }
        
        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('获取博客列表失败：未知的响应格式');
        this.posts = [];
        this.total = 0;
        
      } catch (error) {
        console.error('获取排序博客列表失败:', error);
        this.$message.error('获取博客列表失败：' + (error.message || '网络错误'));
      }
    },
    
    async performSearch() {
      try {
        let apiResponse;
        
        console.log('执行搜索');
        
        let params = {
          page: this.currentPage,
          limit: this.pageSize
        };
        
        if (this.keyword) {
          params.keyword = this.keyword;
          console.log('使用关键词搜索:', this.keyword);
          apiResponse = await SearchBlogs(params);
        } else if (this.tag) {
          params.keyword = this.tag;
          console.log('使用标签搜索:', this.tag);
          apiResponse = await SearchBlogsByTag(params);
        } else if (this.author) {
          params.keyword = this.author;
          console.log('使用作者搜索:', this.author);
          apiResponse = await SearchBlogsByAuthor(params);
        } else {
          this.posts = [];
          this.total = 0;
          return;
        }
        
        console.log('搜索API响应:', apiResponse);
        
        if (apiResponse && typeof apiResponse === 'object') {
          if (Array.isArray(apiResponse)) {
            this.posts = apiResponse;
            this.total = apiResponse.length;
          } else if (apiResponse.data && Array.isArray(apiResponse.data.list)) {
            this.posts = apiResponse.data.list || [];
            this.total = apiResponse.data.total || 0;
          } else if (Array.isArray(apiResponse.data)) {
            this.posts = apiResponse.data || [];
            this.total = apiResponse.data.length || 0;
          } else {
            this.posts = apiResponse.data || [];
            this.total = apiResponse.data?.length || 0;
          }
          return;
        }
        
        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('搜索博客失败：未知的响应格式');
        this.posts = [];
        this.total = 0;
        
      } catch (error) {
        console.error('执行搜索失败:', error);
        this.$message.error('搜索博客失败：' + (error.message || '网络错误'));
      }
    },
    
    async fetchPosts() {
      this.loading = true;
      try {
        console.log('开始获取博客列表...');
        
        if (this.keyword || this.tag || this.author) {
          await this.performSearch();
        } else {
          await this.getSortedBlogs();
        }
        
      } catch (error) {
        console.error('获取博客列表失败:', error);
        this.$message.error('获取博客列表失败：' + (error.message || '网络错误'));
      } finally {
        this.loading = false;
      }
    },

    handlePageChange(page) {
      console.log('页码变化:', page);
      this.currentPage = page;
    },
  },
};
</script>

<style scoped>
/* ========== 全局样式 ========== */
.blog-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #1e293b;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  padding: 30px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ========== 页面头部 ========== */
.blog-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* ========== 排序工具栏 ========== */
.sort-toolbar {
  margin-bottom: 30px;
  padding: 15px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.sort-label {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.sort-group :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  color: #64748b;
  padding: 8px 16px;
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

/* ========== 博客网格 ========== */
.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.blog-card {
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  background: white;
}

.blog-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05);
  border-color: rgba(59, 130, 246, 0.2);
}

.card-content {
  padding: 20px;
}

/* 标题 */
.blog-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px;
  line-height: 1.4;
  transition: color 0.2s ease;
}

.blog-card:hover .blog-title {
  color: #3b82f6;
}

/* 作者信息 */
.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  border: 2px solid #e2e8f0;
  transition: border-color 0.3s ease;
}

.blog-card:hover .author-avatar {
  border-color: #3b82f6;
}

.author-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

/* 标签区域 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-item {
  background: #f1f5f9;
  border: none;
  color: #475569;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-item:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
}

/* 内容预览 */
.blog-excerpt {
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
.post-stats {
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
  font-size: 64px;
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
  .blog-container {
    padding: 20px 15px;
  }

  .page-title {
    font-size: 28px;
  }

  .blog-grid {
    grid-template-columns: 1fr;
  }

  .sort-wrapper {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
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
  .post-stats {
    flex-wrap: wrap;
    gap: 10px;
  }

  .blog-title {
    font-size: 16px;
  }
}
</style>