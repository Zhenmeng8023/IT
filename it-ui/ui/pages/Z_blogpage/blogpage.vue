<template>
  <div class="blog-list" v-loading="loading">
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
          delete newQuery.page; // 明确删除 page 参数
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
  },
  watch: {
    // 监听路由参数变化，重新获取数据
    '$route.query': {
      handler: 'fetchPosts',
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
    async fetchPosts() {
      this.loading = true;
      try {
        let apiResponse;  // 重命名变量，避免混淆
        
        console.log('开始获取博客列表...');
        console.log('当前搜索条件:');
        console.log('- keyword:', this.keyword);
        console.log('- tag:', this.tag);
        console.log('- page:', this.currentPage);
        
        if (this.keyword) {
          console.log('使用关键词搜索:', this.keyword);
          apiResponse = await this.$axios.get('/api/blog/search', {
            params: { keyword: this.keyword }
          });
        } else if (this.tag) {
          console.log('使用标签搜索:', this.tag);
          apiResponse = await this.$axios.get('/api/blog/search/tag', {
            params: { keyword: this.tag }
          });
        } else {
          console.log('获取所有博客');
          apiResponse = await this.$axios.get('/api/blog');
        }
        
        console.log('原始API响应:', apiResponse);
        console.log('响应类型:', typeof apiResponse);
        console.log('是数组吗:', Array.isArray(apiResponse));
        
        // 情况1: apiResponse 本身就是数组
        if (Array.isArray(apiResponse)) {
          console.log('情况1: apiResponse本身就是数组，长度:', apiResponse.length);
          this.total = apiResponse.length;
          
          // 手动分页处理
          const startIndex = (this.currentPage - 1) * this.pageSize;
          const endIndex = startIndex + this.pageSize;
          this.posts = apiResponse.slice(startIndex, endIndex);
          
          console.log('分页后显示的博客:', this.posts.length, '条');
          console.log('第一条博客数据:', this.posts[0]);
          return;
        }
        
        // 情况2: apiResponse 是 axios 响应对象
        if (apiResponse && typeof apiResponse === 'object' && apiResponse.data !== undefined) {
          console.log('情况2: apiResponse是axios响应对象');
          console.log('HTTP状态码:', apiResponse.status);
          console.log('响应数据:', apiResponse.data);
          
          if (Array.isArray(apiResponse.data)) {
            console.log('响应数据是数组，长度:', apiResponse.data.length);
            this.total = apiResponse.data.length;
            
            // 手动分页处理
            const startIndex = (this.currentPage - 1) * this.pageSize;
            const endIndex = startIndex + this.pageSize;
            this.posts = apiResponse.data.slice(startIndex, endIndex);
            
            console.log('分页后显示的博客:', this.posts.length, '条');
            console.log('第一条博客数据:', this.posts[0]);
            return;
          } else {
            console.error('响应数据不是数组:', apiResponse.data);
            this.$message.error('获取博客列表失败：响应数据格式错误');
            this.posts = [];
            this.total = 0;
            return;
          }
        }
        
        // 情况3: 未知格式
        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('获取博客列表失败：未知的响应格式');
        this.posts = [];
        this.total = 0;
        
      } catch (error) {
        console.error('获取博客列表失败:', error);
        if (error.response) {
          console.error('错误响应:', error.response.data);
          console.error('错误状态:', error.response.status);
        } else if (error.request) {
          console.error('无响应:', error.request);
        } else {
          console.error('请求错误:', error.message);
        }
        this.$message.error('获取博客列表失败：' + (error.message || '网络错误'));
      } finally {
        this.loading = false;
      }
    },
    handlePageChange(page) {
      console.log('页码变化:', page);
      this.currentPage = page; // setter 会自动更新路由
    },
  },
};
</script>

<style scoped>
.blog-list {
  padding: 20px;
}

.blog-card-item {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.blog-card-item:hover {
  transform: translateY(-5px);
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}
</style>