<template>
  <div class="blog-list" v-loading="loading">
    <!-- 排序工具栏 -->
    <div class="sort-toolbar">
      <span class="sort-label">排序：</span>
      <el-radio-group v-model="sortType" size="small" @change="handleSortChange">
        <el-radio-button label="hot">按热度</el-radio-button>
        <el-radio-button label="time_desc">最新发布</el-radio-button>
        <el-radio-button label="time_asc">最早发布</el-radio-button>
      </el-radio-group>
    </div>

    <el-card
      v-for="post in posts"
      :key="post.id"
      class="blog-card-item"
      shadow="hover"
      @click.native="goToDetail(post.id)"
    >
      <h3>{{ post.title || '无标题' }}</h3>
     
      <p style="color: #909399; font-size: 14px;">作者：{{ post.author ? post.author.nickname : '未知作者' }}</p>
      
      <!-- 标签展示区域 -->
      <div class="tags-container" v-if="post.tags && post.tags.length > 0">
        <el-tag
          v-for="tag in post.tags"
          :key="tag"
          size="small"
          class="tag-item"
          @click.stop="filterByTag(tag)"
          style="margin-right: 5px; cursor: pointer; margin-bottom: 10px;"
        >
          {{ tag }}
        </el-tag>
      </div>
      
      <p>{{ post.content ? (post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content) : '' }}</p>
      
      <!-- 博客统计信息（用于热度排序） -->
      <div class="post-stats" v-if="post.viewCount !== undefined || post.likeCount !== undefined || post.commentCount !== undefined">
        <span v-if="post.viewCount !== undefined" class="stat-item">
          <i class="el-icon-view"></i> {{ post.viewCount }}
        </span>
        <span v-if="post.likeCount !== undefined" class="stat-item">
          <i class="el-icon-star-off"></i> {{ post.likeCount }}
        </span>
        <span v-if="post.commentCount !== undefined" class="stat-item">
          <i class="el-icon-chat-line-round"></i> {{ post.commentCount }}
        </span>
      </div>
    </el-card>

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
export default {
  layout:'blog',
  data() {
    return {
      posts: [],
      total: 0,
      pageSize: 5,
      loading: false,
      sortType: 'time_desc', // 默认按发布时间倒序（最新）
    };
  },
  computed: {
    // 从路由 query 获取当前页
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
        
        // 如果查询对象为空，则不包含 query
        if (Object.keys(newQuery).length === 0) {
          this.$router.push({
            path: this.$route.path
          });
        } else {
          this.$router.push({
            query: newQuery
          });
        }
      },
    },
    // 搜索关键词
    keyword() {
      return this.$route.query.keyword || '';
    },
    // 标签
    tag() {
      return this.$route.query.tag || '';
    },
    // 从路由获取排序类型
    sortFromRoute() {
      return this.$route.query.sort || 'time_desc';
    },
  },
  watch: {
    // 监听路由参数变化，重新获取数据
    '$route.query': {
      handler() {
        // 从路由同步排序类型
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
    // 添加点击标签进行筛选的方法
    filterByTag(tag) {
      this.$router.push({
        query: {
          ...this.$route.query,
          tag: tag,
          page: 1, // 重置到第一页
        }
      });
    },
    
    // 处理排序变化
    handleSortChange() {
      this.$router.push({
        query: {
          ...this.$route.query,
          sort: this.sortType,
          page: 1, // 重置到第一页
        }
      });
    },
    
    async fetchPosts() {
      this.loading = true;
      try {
        let apiResponse;
        
        console.log('开始获取博客列表...');
        console.log('当前搜索条件:');
        console.log('- keyword:', this.keyword);
        console.log('- tag:', this.tag);
        console.log('- sort:', this.sortType);
        console.log('- page:', this.currentPage);
        
        // 构建请求参数
        const params = {
          page: this.currentPage,
          limit: this.pageSize,
          sort: this.sortType, // 排序参数
        };
        
        // 根据搜索类型添加参数
        if (this.keyword) {
          console.log('使用关键词搜索:', this.keyword);
          params.keyword = this.keyword;
          apiResponse = await this.$axios.get('/api/blog/search', { params });
        } else if (this.tag) {
          console.log('使用标签搜索:', this.tag);
          params.keyword = this.tag;
          apiResponse = await this.$axios.get('/api/blog/search/tag', { params });
        } else {
          console.log('获取所有博客');
          apiResponse = await this.$axios.get('/api/blog', { params });
        }
        
        console.log('原始API响应:', apiResponse);
        
        // 处理响应数据（保持原有逻辑）
        if (Array.isArray(apiResponse)) {
          console.log('情况1: apiResponse本身就是数组，长度:', apiResponse.length);
          
          // 本地排序处理（如果后端不支持排序）
          let sortedData = [...apiResponse];
          if (this.sortType === 'hot') {
            // 按热度排序（假设有 viewCount、likeCount 等字段）
            sortedData.sort((a, b) => {
              const heatA = (a.viewCount || 0) + (a.likeCount || 0) * 2 + (a.commentCount || 0) * 3;
              const heatB = (b.viewCount || 0) + (b.likeCount || 0) * 2 + (b.commentCount || 0) * 3;
              return heatB - heatA;
            });
          } else if (this.sortType === 'time_desc') {
            sortedData.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
          } else if (this.sortType === 'time_asc') {
            sortedData.sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
          }
          
          this.total = sortedData.length;
          const startIndex = (this.currentPage - 1) * this.pageSize;
          const endIndex = startIndex + this.pageSize;
          this.posts = sortedData.slice(startIndex, endIndex);
          return;
        }
        
        // 情况2: apiResponse 是 axios 响应对象
        if (apiResponse && typeof apiResponse === 'object' && apiResponse.data !== undefined) {
          console.log('情况2: apiResponse是axios响应对象');
          
          // 假设后端返回格式为 { code:0, data: { list: [], total: 100 } }
          if (apiResponse.data.code === 0) {
            const responseData = apiResponse.data.data;
            this.posts = responseData.list || [];
            this.total = responseData.total || 0;
          } else if (Array.isArray(apiResponse.data)) {
            console.log('响应数据是数组，长度:', apiResponse.data.length);
            this.total = apiResponse.data.length;
            const startIndex = (this.currentPage - 1) * this.pageSize;
            const endIndex = startIndex + this.pageSize;
            this.posts = apiResponse.data.slice(startIndex, endIndex);
          } else {
            console.error('响应数据格式错误:', apiResponse.data);
            this.$message.error('获取博客列表失败：响应数据格式错误');
            this.posts = [];
            this.total = 0;
          }
          return;
        }
        
        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('获取博客列表失败：未知的响应格式');
        this.posts = [];
        this.total = 0;
        
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
.blog-list {
  padding: 20px;
}

/* 排序工具栏样式 */
.sort-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.sort-label {
  font-size: 14px;
  color: #606266;
  margin-right: 15px;
}

.blog-card-item {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.blog-card-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.tags-container {
  margin-bottom: 10px;
}

.tag-item {
  cursor: pointer;
}

.tag-item:hover {
  opacity: 0.8;
}

/* 博客统计信息样式 */
.post-stats {
  display: flex;
  gap: 20px;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  color: #909399;
  font-size: 13px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item i {
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .sort-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .post-stats {
    flex-wrap: wrap;
    gap: 15px;
  }
}
</style>